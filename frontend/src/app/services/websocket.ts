import { Injectable } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private client: Client;
  private connected = false;

  constructor() {
    this.client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        this.connected = true;
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame);
      }
    });
    this.client.activate();
  }

  /** Subscribe to an upload's status topic. Returns a stream of parsed messages. */
  watchUpload(uploadId: string): Observable<any> {
    const subject = new Subject<any>();

    const trySubscribe = () => {
      if (this.client.connected) {
        const sub = this.client.subscribe(`/topic/uploads/${uploadId}`, (msg: IMessage) => {
          subject.next(JSON.parse(msg.body));
        });
        // clean up the STOMP subscription when the Observable is unsubscribed
        return () => sub.unsubscribe();
      } else {
        // not connected yet — retry shortly
        const timer = setTimeout(trySubscribe, 500);
        return () => clearTimeout(timer);
      }
    };

    let cleanup = trySubscribe();
    return new Observable(observer => {
      const passthrough = subject.subscribe(observer);
      return () => {
        passthrough.unsubscribe();
        if (cleanup) cleanup();
      };
    });
  }
}
