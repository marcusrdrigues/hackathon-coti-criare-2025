import { Pipe, PipeTransform } from '@angular/core';
import { mascararCnpj } from './formatos';

@Pipe({ name: 'cnpj' })
export class CnpjPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return valor ? mascararCnpj(valor) : '';
  }
}
