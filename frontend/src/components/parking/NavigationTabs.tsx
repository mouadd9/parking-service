import { Button } from '@/components/ui/button';
import { MapPin, Clock } from 'lucide-react';

interface NavigationTabsProps {
  activeTab: 'home' | 'history';
  onTabChange: (tab: 'home' | 'history') => void;
}

const NavigationTabs = ({ activeTab, onTabChange }: NavigationTabsProps) => {
  return (
    <div className="flex gap-1 p-1 bg-card border border-border rounded-lg">
      <Button
        variant={activeTab === 'home' ? 'default' : 'ghost'}
        size="sm"
        onClick={() => onTabChange('home')}
        className="flex-1 gap-2"
      >
        <MapPin className="h-4 w-4" />
        Map
      </Button>
      <Button
        variant={activeTab === 'history' ? 'default' : 'ghost'}
        size="sm"
        onClick={() => onTabChange('history')}
        className="flex-1 gap-2"
      >
        <Clock className="h-4 w-4" />
        History
      </Button>
    </div>
  );
};

export default NavigationTabs;