import { CATEGORY_ICONS } from '../../constants/categoryMeta';

interface IconPickerProps {
  value: string | null;
  onChange: (icon: string) => void;
}

/** A compact grid of preset emoji used as category icons. */
function IconPicker({ value, onChange }: IconPickerProps) {
  return (
    <div className="grid grid-cols-8 gap-1.5" role="listbox" aria-label="Category icon">
      {CATEGORY_ICONS.map((icon) => {
        const selected = icon === value;
        return (
          <button
            key={icon}
            type="button"
            role="option"
            aria-selected={selected}
            onClick={() => onChange(icon)}
            className={`flex h-9 w-9 items-center justify-center rounded-lg text-lg transition-all ${
              selected
                ? 'bg-primary-100 ring-2 ring-primary-500'
                : 'bg-gray-50 hover:bg-gray-100'
            }`}
          >
            {icon}
          </button>
        );
      })}
    </div>
  );
}

export default IconPicker;
