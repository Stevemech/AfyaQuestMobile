// Minimal Google Maps types for Places Autocomplete
declare namespace google.maps {
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
      geometry?: {
        location: {
          lat(): number;
          lng(): number;
        };
      };
    }
  }
}
