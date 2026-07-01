import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, shareReplay } from 'rxjs/operators';
import { Interest } from '../model/interest';
import { ResourceHelper } from '../util/servicehelper';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class InterestService {

  private helper = new ResourceHelper();

  private interestUrl = `${environment.API_URL}/api/v1/security/interest`;

  private allInterestRequest$: Observable<Interest[]> | null = null;

  constructor(private http: HttpClient) { }

  getAllInterest() {
    if (!this.allInterestRequest$) {
      this.allInterestRequest$ = this.http.get<Interest[]>(this.interestUrl, {
        headers: this.helper.getHeadersWithToken()
      }).pipe(
        shareReplay(1),
        finalize(() => this.allInterestRequest$ = null)
      );
    }
    return this.allInterestRequest$;
  }

  deleteByIds(ids: string) {
    const url = `${this.interestUrl}?ids=${ids}`;
    return this.http.delete(url, {
      headers: this.helper.getHeadersWithToken()
    });
  }

  download() {
    const url = `${this.interestUrl}/csv`;
    return this.http.get(url, {
      headers: this.helper.getHeadersWithToken(),
      responseType: 'blob'
    });
  }

  create(interest: Interest) {
    return this.http.post<Interest>(this.interestUrl, JSON.stringify(interest), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  update(interest: Interest) {
    return this.http.put<Interest>(this.interestUrl, JSON.stringify(interest), {
      headers: this.helper.getHeadersWithToken(),
    });
  }

  uploadCSV(file: File) {
    const formData = new FormData();
    formData.append('file', file, 'file.csv')
    const url = `${this.interestUrl}/process/csv`;
    return this.http.post<any>(url, formData, {
      headers: this.helper.getAuthHeaders(),
    });
  }
}
