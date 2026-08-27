import { MarketExchange, MarketHoliday } from '../model/market';

export interface MarketStatus {
  open: boolean;
  detail: string;
}

interface TradingWindow {
  openMinutes: number;
  closeMinutes: number;
}

const MINUTES_IN_DAY = 24 * 60;
const MAX_LOOKAHEAD_DAYS = 14;

export function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':');
  return Number(hours) * 60 + Number(minutes);
}

export function localDate(timeZone: string, at: Date): string {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone, year: 'numeric', month: '2-digit', day: '2-digit'
  }).formatToParts(at);
  const value = (type: string) => parts.find(part => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
}

export function localMinutes(timeZone: string, at: Date): number {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone, hour: '2-digit', minute: '2-digit', hour12: false
  }).formatToParts(at);
  const value = (type: string) => Number(parts.find(part => part.type === type)?.value ?? 0);
  return (value('hour') % 24) * 60 + value('minute');
}

export function localTimeLabel(timeZone: string, at: Date): string {
  const minutes = localMinutes(timeZone, at);
  return `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`;
}

function pad(value: number): string {
  return String(value).padStart(2, '0');
}

function shiftDate(date: string, days: number): string {
  const shifted = new Date(`${date}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

function isWeekend(date: string): boolean {
  const day = new Date(`${date}T00:00:00Z`).getUTCDay();
  return day === 0 || day === 6;
}

function holidayOn(exchange: MarketExchange, date: string): MarketHoliday | undefined {
  return exchange.holidays.find(holiday => holiday.holidayDate === date);
}

function tradingWindow(exchange: MarketExchange, date: string): TradingWindow | null {
  if (isWeekend(date)) return null;
  const holiday = holidayOn(exchange, date);
  if (holiday && !holiday.closeTime) return null;
  return {
    openMinutes: toMinutes(exchange.openTime),
    closeMinutes: toMinutes(holiday?.closeTime ?? exchange.closeTime)
  };
}

export function formatGap(minutes: number): string {
  if (minutes >= MINUTES_IN_DAY) {
    const days = Math.floor(minutes / MINUTES_IN_DAY);
    const hours = Math.floor((minutes % MINUTES_IN_DAY) / 60);
    return hours > 0 ? `${days}d ${hours}h` : `${days}d`;
  }
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    return `${hours}h ${pad(minutes % 60)}m`;
  }
  return `${Math.max(minutes, 1)}m`;
}

function minutesUntilOpen(exchange: MarketExchange, today: string, nowMinutes: number): number | null {
  const todayWindow = tradingWindow(exchange, today);
  if (todayWindow && nowMinutes < todayWindow.openMinutes) {
    return todayWindow.openMinutes - nowMinutes;
  }
  for (let offset = 1; offset <= MAX_LOOKAHEAD_DAYS; offset++) {
    const window = tradingWindow(exchange, shiftDate(today, offset));
    if (window) {
      return (MINUTES_IN_DAY - nowMinutes) + (offset - 1) * MINUTES_IN_DAY + window.openMinutes;
    }
  }
  return null;
}

export function resolveMarketStatus(exchange: MarketExchange, at: Date): MarketStatus {
  const today = localDate(exchange.timeZone, at);
  const nowMinutes = localMinutes(exchange.timeZone, at);
  const window = tradingWindow(exchange, today);

  if (window && nowMinutes >= window.openMinutes && nowMinutes < window.closeMinutes) {
    const holiday = holidayOn(exchange, today);
    const gap = formatGap(window.closeMinutes - nowMinutes);
    return {
      open: true,
      detail: holiday?.closeTime ? `closes early in ${gap}` : `closes in ${gap}`
    };
  }

  const holiday = holidayOn(exchange, today);
  if (holiday && !holiday.closeTime) {
    return { open: false, detail: holiday.name };
  }

  const untilOpen = minutesUntilOpen(exchange, today, nowMinutes);
  return {
    open: false,
    detail: untilOpen === null ? 'closed' : `opens in ${formatGap(untilOpen)}`
  };
}

export function upcomingHolidays(exchanges: MarketExchange[], limit: number) {
  return exchanges
    .flatMap(exchange => exchange.holidays.map(holiday => ({ code: exchange.code, holiday })))
    .sort((a, b) => a.holiday.holidayDate.localeCompare(b.holiday.holidayDate))
    .slice(0, limit);
}
