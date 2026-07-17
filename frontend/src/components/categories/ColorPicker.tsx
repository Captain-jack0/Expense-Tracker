import { CATEGORY_COLORS } from '../../constants/categoryMeta';

interface ColorPickerProps {
  value: string | null;
  onChange: (color: string) => void;
}

/** A row of preset color swatches. */
function ColorPicker({ value, onChange }: ColorPickerProps) {
  return (
    <div className="flex flex-wrap gap-2" role="listbox" aria-label="Category color">
      {CATEGORY_COLORS.map((color) => {
        const selected = color === value;
        return (
          <button
            key={color}
            type="button"
            role="option"
            aria-selected={selected}
            aria-label={color}
            onClick={() => onChange(color)}
            style={{ backgroundColor: color }}
            className={`h-8 w-8 rounded-full transition-transform ${
              selected ? 'ring-2 ring-offset-2 ring-gray-700 scale-110' : 'hover:scale-105'
            }`}
          />
        );
      })}
    </div>
  );
}

export default ColorPicker;
