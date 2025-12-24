import { useState, useEffect } from 'react';
import { ParkingSession } from '@/types/parking';
import { Button } from '@/components/ui/button';
import { Clock, MapPin, StopCircle, Car, Loader2 } from 'lucide-react';

interface ActiveSessionBannerProps {
  session: ParkingSession;
  onStopSession: () => void;
  isStoppingSession: boolean;
  hourlyRate: number;
}

const formatDuration = (startTime: Date): { text: string; hours: number } => {
  const now = new Date();
  const diffMs = now.getTime() - startTime.getTime();
  const hours = diffMs / 3600000;
  const fullHours = Math.floor(hours);
  const minutes = Math.floor((diffMs % 3600000) / 60000);
  const seconds = Math.floor((diffMs % 60000) / 1000);
  
  return {
    text: fullHours > 0 ? `${fullHours}h ${minutes}m ${seconds}s` : `${minutes}m ${seconds}s`,
    hours: Math.max(hours, 1),
  };
};

const ActiveSessionBanner = ({ session, onStopSession, isStoppingSession, hourlyRate }: ActiveSessionBannerProps) => {
  const [duration, setDuration] = useState(
    session.startTime ? formatDuration(session.startTime) : { text: '0m 0s', hours: 0 }
  );

  useEffect(() => {
    // Only run timer if session is active (has startTime)
    if (!session.startTime || session.status !== 'active') return;

    const interval = setInterval(() => {
      setDuration(formatDuration(session.startTime!));
    }, 1000);

    return () => clearInterval(interval);
  }, [session.startTime, session.status]);

  const currentCost = (duration.hours * hourlyRate).toFixed(2);

  // Reserved state - waiting for entry detection
  if (session.status === 'reserved') {
    return (
      <div className="bg-card border border-border rounded-lg p-4">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-amber-500 text-white">
              <Car className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <p className="text-sm font-medium text-foreground truncate">{session.zoneName}</p>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span>Spot {session.spotNumber}</span>
              </div>
              {/* 
                SENSOR INTEGRATION NOTE:
                This message is shown while waiting for the sensor to detect the vehicle.
                Once detected, session.status changes to 'active' and timer starts.
              */}
              <div className="flex items-center gap-1.5 text-xs text-amber-600 font-medium mt-0.5">
                <Loader2 className="h-3 w-3 animate-spin" />
                Waiting for entry - Timer starts when you park
              </div>
            </div>
          </div>
          <Button
            variant="outline"
            size="sm"
            onClick={onStopSession}
            disabled={isStoppingSession}
            className="shrink-0 border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
          >
            Cancel
          </Button>
        </div>
      </div>
    );
  }

  // Active state - timer running
  return (
    <div className="bg-card border border-border rounded-lg p-4">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3 min-w-0">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground">
            <Clock className="h-5 w-5" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-medium text-foreground truncate">{session.zoneName}</p>
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <span>Spot {session.spotNumber}</span>
              <span>·</span>
              <span className="font-mono">{duration.text}</span>
            </div>
            <div className="text-xs text-primary font-medium mt-0.5">
              ~{currentCost} MAD
            </div>
            {/* 
              SENSOR INTEGRATION NOTE:
              In production, the timer stops automatically when the sensor detects the vehicle leaving.
              The "Stop" button below is for manual override or demo purposes.
              Backend API: POST /api/sensors/exit-detected
            */}
          </div>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={onStopSession}
          disabled={isStoppingSession}
          className="shrink-0 border-destructive text-destructive hover:bg-destructive hover:text-destructive-foreground"
        >
          <StopCircle className="h-4 w-4 mr-1.5" />
          Stop
        </Button>
      </div>
    </div>
  );
};

export default ActiveSessionBanner;