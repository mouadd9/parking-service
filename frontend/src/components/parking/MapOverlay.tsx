import { ReactNode } from 'react';

interface MapOverlayProps {
  position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
  children: ReactNode;
}

const positionClasses = {
  'top-left': 'top-4 left-4',
  'top-right': 'top-4 right-4',
  'bottom-left': 'bottom-4 left-4',
  'bottom-right': 'bottom-4 right-4',
};

const MapOverlay = ({ position, children }: MapOverlayProps) => {
  return (
    <div 
      className={`absolute ${positionClasses[position]} z-10`}
      style={{ pointerEvents: 'auto' }}
    >
      {children}
    </div>
  );
};

export default MapOverlay;