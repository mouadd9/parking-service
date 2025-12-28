import { useEffect, useState } from "react";
import { Dialog, DialogContent } from "@/components/ui/dialog";
import { Clock, Car } from "lucide-react";

interface WaitingToEnterPopupProps {
  isOpen: boolean;
  spotNumber: string;
  zoneName: string;
  onComplete: () => void;
}

export default function WaitingToEnterPopup({
  isOpen,
  spotNumber,
  zoneName,
  onComplete,
}: WaitingToEnterPopupProps) {
  const [countdown, setCountdown] = useState(0);
  const [randomDuration] = useState(() => {
    // Random duration between 10 and 15 seconds
    return Math.floor(Math.random() * (15 - 10 + 1)) + 10;
  });

  useEffect(() => {
    if (!isOpen) {
      setCountdown(0);
      return;
    }

    setCountdown(randomDuration);

    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          onComplete();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isOpen, randomDuration, onComplete]);

  return (
    <Dialog open={isOpen} onOpenChange={() => {}}>
      <DialogContent className="max-w-sm border-border bg-card" closeButton={false}>
        <div className="flex flex-col items-center gap-4 py-6">
          <div className="relative">
            <div className="absolute inset-0 bg-primary/20 rounded-full animate-ping" />
            <div className="relative bg-primary/10 rounded-full p-6">
              <Car className="w-12 h-12 text-primary animate-pulse" />
            </div>
          </div>

          <div className="text-center space-y-2">
            <h3 className="text-lg font-semibold">Waiting for you to enter</h3>
            <p className="text-sm text-muted-foreground">
              Please proceed to spot <span className="font-medium">{spotNumber}</span>
            </p>
            <p className="text-xs text-muted-foreground">{zoneName}</p>
          </div>

          <div className="flex items-center gap-3">
            <Clock className="w-5 h-5 text-primary" />
            <span className="text-2xl font-mono font-bold text-primary">
              {countdown}s
            </span>
          </div>

          <div className="w-full bg-muted rounded-full h-2 overflow-hidden">
            <div
              className="bg-primary h-full transition-all duration-1000 ease-linear"
              style={{
                width: `${((randomDuration - countdown) / randomDuration) * 100}%`,
              }}
            />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

