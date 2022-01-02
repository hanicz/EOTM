import { HttpHeaders } from '@angular/common/http';

export class ResourceHelper {
    
    private headers = new HttpHeaders({
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    });

    private authHeaders = new HttpHeaders({
    });

    getHeadersWithToken = () => {
        return this.headers.append('Authorization', <string>localStorage.getItem('token'));
    }

    getAuthHeaders = () => {
        return this.authHeaders.append('Authorization', <string>localStorage.getItem('token'));
    }
}