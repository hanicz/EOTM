export type Signal = 'BUY' | 'SELL' | 'HOLD';
export type IndicatorVote = 'BUY' | 'SELL' | 'NEUTRAL';

export interface IndicatorDetail {
  name: string;
  description: string;
  vote: IndicatorVote;
  detail: string;
}

export interface SignalResult {
  signal: Signal;
  indicators: IndicatorDetail[];
}
