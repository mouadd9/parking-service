import { ParkingSession } from '@/types/parking';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { CheckCircle, Clock, MapPin, CreditCard, Receipt } from 'lucide-react';

interface PaymentSummaryModalProps {
  session: ParkingSession;
  isOpen: boolean;
  onClose: () => void;
}

const formatDuration = (startTime: Date, endTime: Date): string => {
  const diffMs = endTime.getTime() - startTime.getTime();
  const hours = Math.floor(diffMs / 3600000);
  const minutes = Math.floor((diffMs % 3600000) / 60000);
  
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;
};

const PaymentSummaryModal = ({ session, isOpen, onClose }: PaymentSummaryModalProps) => {
  const duration = session.endTime 
    ? formatDuration(session.startTime, session.endTime)
    : '0m';

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-sm border-border bg-card">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 justify-center">
            <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
              <CheckCircle className="h-6 w-6 text-primary" />
            </div>
          </DialogTitle>
        </DialogHeader>
        
        <div className="text-center space-y-2 pb-4">
          <h3 className="text-xl font-semibold text-foreground">Payment Successful</h3>
          <p className="text-sm text-muted-foreground">Your parking session has ended</p>
        </div>

        <div className="space-y-4">
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
              <span className="font-medium text-foreground">{duration}</span>
            </div>
            <div className="border-t border-border pt-3 mt-3">
              <div className="flex justify-between">
                <span className="text-muted-foreground flex items-center gap-1.5">
                  <CreditCard className="h-3.5 w-3.5" />
                  Amount Charged
                </span>
                <span className="font-bold text-foreground text-xl">{session.totalCost?.toFixed(2)} MAD</span>
              </div>
            </div>
          </div>
          
          <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground">
            <Receipt className="h-3.5 w-3.5" />
            <span>Receipt sent to your email</span>
          </div>
        </div>

        <DialogFooter className="pt-4">
          <Button onClick={onClose} className="w-full">
            Done
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default PaymentSummaryModal;
