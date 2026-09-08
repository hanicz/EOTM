export function collectAccountOptions(rows: { accountName?: string }[]): string[] {
  return Array.from(new Set(rows.map(row => row.accountName).filter((name): name is string => !!name))).sort();
}
