// Minimal Google Maps types for Places Autocomplete + Map rendering
declare namespace google.maps {
  class Map {
    constructor(element: HTMLElement, opts?: MapOptions);
    fitBounds(bounds: LatLngBounds, padding?: number | { top: number; right: number; bottom: number; left: number }): void;
    setCenter(latlng: LatLngLiteral): void;
    setZoom(zoom: number): void;
  }

  class Marker {
    constructor(opts?: MarkerOptions);
    addListener(event: string, handler: () => void): void;
    setMap(map: Map | null): void;
  }

  class InfoWindow {
    constructor(opts?: { content?: string });
    open(map: Map, anchor?: Marker): void;
    close(): void;
  }

  class LatLngBounds {
    constructor();
    extend(point: LatLngLiteral): LatLngBounds;
  }

  interface LatLngLiteral {
    lat: number;
    lng: number;
  }

  interface MapOptions {
    center?: LatLngLiteral;
    zoom?: number;
    mapTypeControl?: boolean;
    streetViewControl?: boolean;
    fullscreenControl?: boolean;
    zoomControl?: boolean;
    styles?: Array<{ featureType?: string; elementType?: string; stylers: Array<Record<string, string>> }>;
  }

  interface MarkerOptions {
    position?: LatLngLiteral;
    map?: Map;
    title?: string;
    icon?: string | {
      path: number;
      scale?: number;
      fillColor?: string;
      fillOpacity?: number;
      strokeColor?: string;
      strokeWeight?: number;
    };
  }

  const SymbolPath: {
    CIRCLE: number;
    FORWARD_CLOSED_ARROW: number;
    FORWARD_OPEN_ARROW: number;
    BACKWARD_CLOSED_ARROW: number;
    BACKWARD_OPEN_ARROW: number;
  };

  namespace places {
    class Autocomplete {
      constructor(input: HTMLInputElement, opts?: AutocompleteOptions);
      addListener(event: string, handler: () => void): void;
      getPlace(): PlaceResult;
    }
    interface AutocompleteOptions {
      types?: string[];
      fields?: string[];
    }
    interface PlaceResult {
      formatted_address?: string;
      name?: string;
      geometry?: {
        location: {
          lat(): number;
          lng(): number;
        };
      };
    }
  }
}
