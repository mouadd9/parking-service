import { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Clock, MapPin, DollarSign } from "lucide-react";
import { ParkingSession } from "@/types/parking";

interface ActiveParkingPopupProps {
  isOpen: boolean;
  session: ParkingSession;
  hourlyRate: number;
  onClose?: () => void;
}

export default function ActiveParkingPopup({
  isOpen,
  session,
  hourlyRate,
  onClose,
}: ActiveParkingPopupProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [currentCost, setCurrentCost] = useState(0);

  useEffect(() => {
    if (!isOpen || !session.startTime) {
      setElapsedSeconds(0);
      setCurrentCost(0);
      return;
    }

    // Calculate initial elapsed time
    const updateElapsed = () => {
      const now = new Date().getTime();
      const start = new Date(session.startTime!).getTime();
      const elapsed = Math.floor((now - start) / 1000);
      setElapsedSeconds(elapsed);

      // Calculate cost (minimum 1 hour)
      const hours = Math.max(elapsed / 3600, 1 / 3600); // At least 1 second = some cost
      const cost = hours * hourlyRate;
      setCurrentCost(cost);
    };

    updateElapsed();

    // Update every second
    const interval = setInterval(updateElapsed, 1000);

    return () => clearInterval(interval);
  }, [isOpen, session.startTime, hourlyRate]);

  const formatTime = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, "0")}:${mins
      .toString()
      .padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-sm border-border bg-card">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
            Active Parking Session
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Location Info */}
          <div className="flex items-center gap-3 p-3 bg-muted rounded-lg">
            <MapPin className="w-5 h-5 text-primary" />
            <div className="flex-1">
              <p className="font-semibold text-sm">{session.zoneName}</p>
              <p className="text-xs text-muted-foreground">
                Spot {session.spotNumber}
              </p>
            </div>
          </div>

          {/* Timer */}
          <div className="flex items-center justify-between p-4 bg-primary/5 rounded-lg border border-primary/20">
            <div className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-primary" />
              <span className="text-sm font-medium text-muted-foreground">
                Duration
              </span>
            </div>
            <span className="text-3xl font-mono font-bold text-primary">
              {formatTime(elapsedSeconds)}
            </span>
          </div>

          {/* Cost */}
          <div className="flex items-center justify-between p-4 bg-muted rounded-lg">
            <div className="flex items-center gap-2">
              <DollarSign className="w-5 h-5 text-muted-foreground" />
              <span className="text-sm font-medium text-muted-foreground">
                Current Cost
              </span>
            </div>
            <div className="text-right">
              <span className="text-2xl font-bold text-foreground">
                {currentCost.toFixed(2)}€
              </span>
              <p className="text-xs text-muted-foreground">
                {hourlyRate.toFixed(2)}€/hour
              </p>
            </div>
          </div>

          {/* Info Message */}
          <div className="p-3 bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-800 rounded-lg">
            <p className="text-xs text-blue-700 dark:text-blue-400 text-center">
              ⏱️ Session will end automatically when you exit
            </p>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

