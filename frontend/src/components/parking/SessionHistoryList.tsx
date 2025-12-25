import { ParkingSession } from '@/types/parking';
import { Card } from '@/components/ui/card';
import { format } from 'date-fns';
import { Clock, MapPin } from 'lucide-react';

interface SessionHistoryListProps {
  sessions: ParkingSession[];
}

const SessionHistoryList = ({ sessions }: SessionHistoryListProps) => {
  const completedSessions = sessions.filter(s => s.status === 'completed');

  if (completedSessions.length === 0) {
    return (
      <Card className="p-8 text-center border-border">
        <Clock className="h-12 w-12 mx-auto text-muted-foreground mb-3" />
        <p className="text-sm text-muted-foreground">No parking history yet</p>
      </Card>
    );
  }

  return (
    <div className="space-y-3">
      <h2 className="text-lg font-semibold text-foreground">Parking History</h2>
      {completedSessions.map((session) => (
        <Card key={session.id} className="p-4 border-border">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3 min-w-0">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-secondary text-secondary-foreground">
                <MapPin className="h-5 w-5" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-foreground truncate">{session.zoneName}</p>
                <p className="text-xs text-muted-foreground">Spot {session.spotNumber}</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {format(session.startTime, 'MMM d, yyyy · HH:mm')}
                </p>
              </div>
            </div>
            <div className="text-right shrink-0">
              <p className="text-sm font-semibold text-foreground">{session.totalCost?.toFixed(2)} MAD</p>
              {session.endTime && (
                <p className="text-xs text-muted-foreground">
                  {Math.round((session.endTime.getTime() - session.startTime.getTime()) / 60000)} min
                </p>
              )}
            </div>
          </div>
        </Card>
      ))}
    </div>
  );
};

export default SessionHistoryList;