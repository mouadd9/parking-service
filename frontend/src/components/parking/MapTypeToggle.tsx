import { Button } from '@/components/ui/button';
import { Map, Satellite } from 'lucide-react';

interface MapTypeToggleProps {
  mapType: 'normal' | 'satellite';
  onToggle: (type: 'normal' | 'satellite') => void;
}

const MapTypeToggle = ({ mapType, onToggle }: MapTypeToggleProps) => {
  return (
    <div className="flex gap-1 p-1 bg-card border border-border rounded-lg shadow-sm">
      <Button
        variant={mapType === 'normal' ? 'default' : 'ghost'}
        size="sm"
        onClick={() => onToggle('normal')}
        className="gap-1.5 px-2.5"
      >
        <Map className="h-4 w-4" />
        <span className="sr-only sm:not-sr-only">Map</span>
      </Button>
      <Button
        variant={mapType === 'satellite' ? 'default' : 'ghost'}
        size="sm"
        onClick={() => onToggle('satellite')}
        className="gap-1.5 px-2.5"
      >
        <Satellite className="h-4 w-4" />
        <span className="sr-only sm:not-sr-only">Satellite</span>
      </Button>
    </div>
  );
};

export default MapTypeToggle;
