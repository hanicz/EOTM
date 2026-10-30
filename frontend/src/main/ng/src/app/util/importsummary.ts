import { ImportResult } from '../model/importResult';

export function describeImport(result: ImportResult, suffix = ''): string {
  const parts = [`${result.created} added`];
  if (result.updated) parts.push(`${result.updated} updated`);
  if (result.unchanged) parts.push(`${result.unchanged} unchanged`);
  return `Import finished. ${parts.join(', ')}${suffix}.`;
}
