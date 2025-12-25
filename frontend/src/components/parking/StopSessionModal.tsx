import { ParkingSession } from '@/types/parking';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AlertTriangle, Clock, MapPin, CreditCard, XCircle } from 'lucide-react';

interface StopSessionModalProps {
  session: ParkingSession;
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  isLoading: boolean;
  hourlyRate: number;
}

const formatDuration = (startTime: Date): { text: string; hours: number } => {
  const now = new Date();
  const diffMs = now.getTime() - startTime.getTime();
  const hours = diffMs / 3600000;
  const fullHours = Math.floor(hours);
  const minutes = Math.floor((diffMs % 3600000) / 60000);
  
  return {
    text: fullHours > 0 ? `${fullHours}h ${minutes}m` : `${minutes}m`,
    hours: Math.max(hours, 1), // Minimum 1 hour charge
  };
};

const StopSessionModal = ({ 
  session, 
  isOpen, 
  onClose, 
  onConfirm, 
  isLoading,
  hourlyRate 
}: StopSessionModalProps) => {
  
  // Handle reserved state (no startTime yet)
  if (session.status === 'reserved' || !session.startTime) {
    return (
      <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
        <DialogContent className="max-w-sm border-border bg-card">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <XCircle className="h-5 w-5 text-amber-500" />
              Cancel Reservation?
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div className="bg-muted rounded-lg p-4 space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground flex items-center gap-1.5">
                  <MapPin className="h-3.5 w-3.5" />
                  Location
                </span>
                <span className="font-medium text-foreground">{session.zoneName}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Spot</span>
                <span className="font-medium text-foreground">{session.spotNumber}</span>
              </div>
            </div>
            
            <p className="text-xs text-muted-foreground text-center">
              You haven't entered the parking yet. No charges will be applied.
            </p>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="outline" onClick={onClose} disabled={isLoading}>
              Keep Reservation
            </Button>
            <Button 
              variant="destructive" 
              onClick={onConfirm} 
              disabled={isLoading}
            >
              {isLoading ? 'Canceling...' : 'Cancel Reservation'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  // Active state - timer is running
  const duration = formatDuration(session.startTime);
  const totalCost = Math.max(duration.hours * hourlyRate, hourlyRate);

  /**
   * SENSOR INTEGRATION NOTE:
   * In production, this modal may be shown when the sensor detects the vehicle leaving,
   * giving the user a final confirmation before payment is processed.
   * Backend API: POST /api/payments/charge
   * Request body: { sessionId: string, amount: number, userId: string }
   */
  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-sm border-border bg-card">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-destructive" />
            Stop Parking Session?
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4 py-4">
          <div className="bg-muted rounded-lg p-4 space-y-3">
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <MapPin className="h-3.5 w-3.5" />
                Location
              </span>
              <span className="font-medium text-foreground">{session.zoneName}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground">Spot</span>
              <span className="font-medium text-foreground">{session.spotNumber}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-muted-foreground flex items-center gap-1.5">
                <Clock className="h-3.5 w-3.5" />
                Duration
              </span>
              <span className="font-medium text-foreground">{duration.text}</span>
            </div>
            <div className="border-t border-border pt-3 mt-3">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground flex items-center gap-1.5">
                  <CreditCard className="h-3.5 w-3.5" />
                  Total
                </span>
                <span className="font-semibold text-foreground text-lg">{totalCost.toFixed(2)} MAD</span>
              </div>
            </div>
          </div>
          
          <p className="text-xs text-muted-foreground text-center">
            This amount will be charged to your saved card.
          </p>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={onClose} disabled={isLoading}>
            Cancel
          </Button>
          <Button 
            variant="destructive" 
            onClick={onConfirm} 
            disabled={isLoading}
          >
            {isLoading ? 'Processing...' : 'Stop & Pay'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default StopSessionModal;