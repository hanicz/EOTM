import { describe, expect, it } from 'vitest';
import { MarketExchange } from '../model/market';
import { formatGap, localTimeLabel, resolveMarketStatus, upcomingHolidays } from './marketstatus';

const us: MarketExchange = {
  code: 'US', name: 'NYSE / NASDAQ', timeZone: 'America/New_York', currency: 'USD',
  countryISO2: 'US', openTime: '09:30', closeTime: '16:00',
  holidays: [
    { holidayDate: '2026-11-26', name: 'Thanksgiving Day', closeTime: null },
    { holidayDate: '2026-11-27', name: 'Day after Thanksgiving', closeTime: '13:00' },
    { holidayDate: '2026-12-25', name: 'Christmas Day', closeTime: null }
  ]
};

const bud: MarketExchange = {
  code: 'BUD', name: 'Budapest Stock Exchange', timeZone: 'Europe/Budapest', currency: 'HUF',
  countryISO2: 'HU', openTime: '09:00', closeTime: '17:00',
  holidays: [{ holidayDate: '2026-10-23', name: '1956 Revolution Memorial Day', closeTime: null }]
};

describe('resolveMarketStatus', () => {

  it('reports open during the session', () => {
    const status = resolveMarketStatus(us, new Date('2026-09-08T14:00:00Z'));
    expect(status.open).toBe(true);
    expect(status.detail).toBe('closes in 6h 00m');
  });

  it('reports closed before the open with a countdown', () => {
    const status = resolveMarketStatus(us, new Date('2026-09-08T12:00:00Z'));
    expect(status.open).toBe(false);
    expect(status.detail).toBe('opens in 1h 30m');
  });

  it('names the holiday when the market is shut for the day', () => {
    const status = resolveMarketStatus(us, new Date('2026-12-25T15:00:00Z'));
    expect(status.open).toBe(false);
    expect(status.detail).toBe('Christmas Day');
  });

  it('honours an early close', () => {
    const openEarly = resolveMarketStatus(us, new Date('2026-11-27T17:00:00Z'));
    expect(openEarly.open).toBe(true);
    expect(openEarly.detail).toBe('closes early in 1h 00m');

    const shut = resolveMarketStatus(us, new Date('2026-11-27T18:30:00Z'));
    expect(shut.open).toBe(false);
  });

  it('skips the weekend when counting to the next open', () => {
    const status = resolveMarketStatus(us, new Date('2026-09-05T15:00:00Z'));
    expect(status.open).toBe(false);
    expect(status.detail).toBe('opens in 1d 22h');
  });

  it('skips a holiday when counting to the next open', () => {
    const status = resolveMarketStatus(bud, new Date('2026-10-22T20:00:00Z'));
    expect(status.open).toBe(false);
    expect(status.detail).toBe('opens in 3d 11h');
  });

  it('is DST aware', () => {
    const winter = resolveMarketStatus(us, new Date('2026-12-24T15:00:00Z'));
    expect(winter.open).toBe(true);
    const summer = resolveMarketStatus(us, new Date('2026-09-08T20:30:00Z'));
    expect(summer.open).toBe(false);
  });
});

describe('localTimeLabel', () => {
  it('renders the exchange wall clock', () => {
    expect(localTimeLabel('America/New_York', new Date('2026-09-08T14:05:00Z'))).toBe('10:05');
    expect(localTimeLabel('Europe/Budapest', new Date('2026-09-08T14:05:00Z'))).toBe('16:05');
  });

  it('renders midnight as 00:00', () => {
    expect(localTimeLabel('Europe/London', new Date('2026-01-01T00:00:00Z'))).toBe('00:00');
  });
});

describe('formatGap', () => {
  it('formats minutes, hours and days', () => {
    expect(formatGap(5)).toBe('5m');
    expect(formatGap(90)).toBe('1h 30m');
    expect(formatGap(1440)).toBe('1d');
    expect(formatGap(1500)).toBe('1d 1h');
  });
});

describe('upcomingHolidays', () => {
  it('merges exchanges and sorts by date', () => {
    const result = upcomingHolidays([us, bud], 2);
    expect(result.map(item => item.code)).toEqual(['BUD', 'US']);
    expect(result[0].holiday.holidayDate).toBe('2026-10-23');
  });
});
