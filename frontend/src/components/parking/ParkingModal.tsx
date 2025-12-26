import { useState } from 'react';
import { ParkingSpot } from '@/types/parking';
import { GeoJSONFeature } from '@/utils/overpassToGeoJSON';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { MapPin, Car, Clock, CreditCard, Loader2 } from 'lucide-react';

interface ParkingModalProps {
  parking: GeoJSONFeature | null;
  spots: ParkingSpot[];
  isOpen: boolean;
  onClose: () => void;
  onBookSpot: (spot: ParkingSpot, hourlyRate: number) => void;
  activeSessionSpotId?: string;
  hasActiveSession: boolean;
  isBooking?: boolean;
}

// Mock hourly rate based on parking type
const getHourlyRate = (parking: GeoJSONFeature): number => {
  const access = parking.properties.access;
  if (access === 'private') return 8.00;
  if (access === 'customers') return 6.00;
  return 5.00; // Default public parking rate
};

const ParkingModal = ({
  parking,
  spots,
  isOpen,
  onClose,
  onBookSpot,
  activeSessionSpotId,
  hasActiveSession,
  isBooking = false
}: ParkingModalProps) => {
  const [selectedSpot, setSelectedSpot] = useState<ParkingSpot | null>(null);
  const [showPaymentConfirm, setShowPaymentConfirm] = useState(false);

  if (!parking) return null;

  const hourlyRate = getHourlyRate(parking);
  const freeSpots = spots.filter(s => s.status === 'free');

  /**
   * SENSOR INTEGRATION NOTE:
   * In the production flow, clicking a spot should:
   * 1. Reserve the spot (show payment confirmation)
   * 2. User confirms payment → Spot is marked as "reserved/pending"
   * 3. Wait for sensor to detect vehicle matricule
   * 4. Only then start the timer
   * 
   * Current mock flow: Spot selection → Payment confirm → Timer starts immediately
   */
  const handleSpotClick = (spot: ParkingSpot) => {
    if (hasActiveSession) return;
    setSelectedSpot(spot);
    setShowPaymentConfirm(true);
  };

  /**
   * SENSOR INTEGRATION NOTE:
   * After payment confirmation, in production:
   * 1. Reserve the spot via API: POST /api/spots/{spotId}/reserve
   * 2. Display "Waiting for vehicle detection..." message
   * 3. Listen for sensor webhook/WebSocket for entry detection
   * 4. On detection → Start the parking timer
   */
  const handleConfirmPayment = () => {
    if (selectedSpot) {
      onBookSpot(selectedSpot, hourlyRate);
      setShowPaymentConfirm(false);
      setSelectedSpot(null);
      onClose();
    }
  };

  const handleCancelPayment = () => {
    setShowPaymentConfirm(false);
    setSelectedSpot(null);
  };

  // Payment Confirmation Dialog
  if (showPaymentConfirm && selectedSpot) {
    return (
      <Dialog open={true} onOpenChange={() => handleCancelPayment()}>
        <DialogContent className="max-w-sm border-border bg-card">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <CreditCard className="h-5 w-5" />
              Confirm Parking
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div className="bg-muted rounded-lg p-4 space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Parking</span>
                <span className="font-medium text-foreground">{parking.properties.name}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Spot</span>
                <span className="font-medium text-foreground">{selectedSpot.spotNumber}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Rate</span>
                <span className="font-medium text-foreground">{hourlyRate.toFixed(2)} MAD/hour</span>
              </div>
            </div>
            
            {/* 
              SENSOR INTEGRATION NOTE:
              This message informs the user that the timer will only start 
              when the parking sensor detects their vehicle entering.
              Backend listens for: POST /api/sensors/entry-detected
            */}
            <div className="p-3 bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800 rounded-lg">
              <p className="text-xs text-amber-700 dark:text-amber-400 text-center font-medium">
                ⏱️ Timer starts when you enter the parking
              </p>
              <p className="text-xs text-amber-600 dark:text-amber-500 text-center mt-1">
                Timer stops automatically when you leave
              </p>
            </div>
            
            <p className="text-xs text-muted-foreground text-center">
              Your card will be charged automatically when you leave.
              Your card will be charged automatically when you stop parking.
            </p>
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="outline" onClick={handleCancelPayment} disabled={isBooking}>
              Cancel
            </Button>
            <Button onClick={handleConfirmPayment} disabled={isBooking}>
              {isBooking ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  Reserving...
                </>
              ) : (
                'Reserve Spot'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md p-0 gap-0 border-border bg-card">
        <DialogHeader className="p-6 pb-4 border-b border-border">
          <DialogTitle className="flex items-start gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
              <MapPin className="h-5 w-5" />
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold text-foreground truncate">{parking.properties.name}</h2>
              <div className="flex items-center gap-4 mt-1 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5" />
                  {hourlyRate.toFixed(2)} MAD/h
                </span>
                <span className="flex items-center gap-1">
                  <Car className="h-3.5 w-3.5" />
                  {freeSpots.length}/{spots.length} available
                </span>
              </div>
            </div>
          </DialogTitle>
        </DialogHeader>

        <div className="p-6 pt-4">
          {hasActiveSession && (
            <div className="mb-4 p-3 bg-destructive/10 text-destructive text-sm rounded-lg">
              You have an active parking session. Stop it before booking a new spot.
            </div>
          )}

          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-medium text-foreground">Select a Spot</span>
            <span className="text-xs text-muted-foreground">{freeSpots.length} available</span>
          </div>

          <ScrollArea className="h-[280px] -mx-2 px-2">
            <div className="grid grid-cols-4 gap-2">
              {spots.map((spot) => {
                const isFree = spot.status === 'free';
                const isBooked = spot.id === activeSessionSpotId;

                return (
                  <div key={spot.id} className="relative">
                    {isFree && !hasActiveSession ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleSpotClick(spot)}
                        className="w-full h-12 text-xs font-medium border-border hover:bg-primary hover:text-primary-foreground hover:border-primary"
                      >
                        {spot.spotNumber}
                      </Button>
                    ) : (
                      <div className={`w-full h-12 flex items-center justify-center text-xs font-medium rounded-md border ${
                        isBooked 
                          ? 'bg-primary text-primary-foreground border-primary' 
                          : 'bg-muted text-muted-foreground border-border'
                      }`}>
                        {spot.spotNumber}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </ScrollArea>

          <div className="flex items-center gap-6 mt-4 pt-4 border-t border-border text-xs text-muted-foreground">
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-sm border border-border" />
              <span>Available</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-sm bg-muted border border-border" />
              <span>Occupied</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="h-3 w-3 rounded-sm bg-primary" />
              <span>Your Spot</span>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default ParkingModal;
