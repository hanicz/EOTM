import { FireProjection } from '../model/fire';

export interface Milestone {
    target: number;
    years: number;
    age: number;
    pct: number;
}

const CHART_DOTS = 4;

export function nextMilestone(projection: FireProjection): Milestone | null {
  const from = projection.startingValue;
  if (from <= 0 || from >= projection.fireNumber) return null;

  const magnitude = Math.pow(10, Math.floor(Math.log10(from)));
  const multiple = [1, 2, 5, 10].find(step => step * magnitude > from) ?? 10;
  const target = Math.min(multiple * magnitude, projection.fireNumber);

  const reached = projection.timeline.find(point => point.balance >= target);
  if (!reached) return null;

  return {
    target: target,
    years: reached.year,
    age: reached.age,
    pct: Math.max(0, Math.min(100, from / target * 100))
  };
}

export function dotYears(projection: FireProjection): number[] {
  const timeline = projection.timeline;
  const end = projection.fiYear ?? timeline[timeline.length - 1].year;
  if (end <= 0) return [0];

  const gap = end / (CHART_DOTS - 1);
  const years = new Set<number>();
  for (let step = 0; step < CHART_DOTS; step++) {
    years.add(Math.round(step * gap));
  }
  years.add(end);
  return [...years].sort((first, second) => first - second);
}
