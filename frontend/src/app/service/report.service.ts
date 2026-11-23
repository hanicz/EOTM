import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { ReportSubscription } from '../model/reportsubscription';

@Injectable({
  providedIn: 'root'
})
export class ReportService {

  private reportUrl = `${environment.API_URL}/api/v1/report`;

  constructor(private http: HttpClient) { }

  getSubscription() {
    const url = `${this.reportUrl}/monthly/subscription`;
    return this.http.get<ReportSubscription>(url);
  };

  updateSubscription(subscription: ReportSubscription) {
    const url = `${this.reportUrl}/monthly/subscription`;
    return this.http.put<ReportSubscription>(url, subscription);
  };

  sendNow() {
    const url = `${this.reportUrl}/monthly/send`;
    return this.http.post(url, {});
  };
}
