export interface MarketHoliday {
  holidayDate: string;
  name: string;
  closeTime: string | null;
}

export interface MarketExchange {
  code: string;
  name: string;
  timeZone: string;
  currency: string;
  openTime: string;
  closeTime: string;
  holidays: MarketHoliday[];
}
