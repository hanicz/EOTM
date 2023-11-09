import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'alertTypePipe'
  })
  export class AlertTypePipe implements PipeTransform {

    transform(value: string): string {
        let ret = value.replace('_', ' ').toLowerCase();
      return ret.charAt(0).toUpperCase() + ret.substring(1);
    }
  }