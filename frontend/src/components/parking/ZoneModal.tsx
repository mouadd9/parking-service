import { ParkingZone, ParkingSpot } from '@/types/parking';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { ScrollArea } from '@/components/ui/scroll-area';
import { MapPin, Car, Clock } from 'lucide-react';

interface ZoneModalProps {
  zone: ParkingZone | null;
  spots: ParkingSpot[];
  isOpen: boolean;
  onClose: () => void;
  onBookSpot: (spot: ParkingSpot) => void;
  activeSessionSpotId?: string;
}

const ZoneModal = ({ zone, spots, isOpen, onClose, onBookSpot, activeSessionSpotId }: ZoneModalProps) => {
  if (!zone) return null;

  const freeSpots = spots.filter(s => s.status === 'free');
  const occupiedSpots = spots.filter(s => s.status === 'occupied' || s.status === 'booked');

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-md p-0 gap-0 border-border bg-card">
        <DialogHeader className="p-6 pb-4 border-b border-border">
          <DialogTitle className="flex items-start gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
              {zone.availableSpots}
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold text-foreground truncate">{zone.name}</h2>
              <div className="flex items-center gap-4 mt-1 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5" />
                  {zone.hourlyRate.toFixed(2)} MAD/h
                </span>
                <span className="flex items-center gap-1">
                  <Car className="h-3.5 w-3.5" />
                  {zone.availableSpots}/{zone.totalSpots}
                </span>
              </div>
            </div>
          </DialogTitle>
        </DialogHeader>

        <div className="p-6 pt-4">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-medium text-foreground">Available Spots</span>
            <span className="text-xs text-muted-foreground">{freeSpots.length} available</span>
          </div>

          <ScrollArea className="h-[280px] -mx-2 px-2">
            <div className="grid grid-cols-4 gap-2">
              {spots.map((spot) => {
                const isFree = spot.status === 'free';
                const isBooked = spot.id === activeSessionSpotId;

                return (
                  <div key={spot.id} className="relative">
                    {isFree ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => onBookSpot(spot)}
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

export default ZoneModal;