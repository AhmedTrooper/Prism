import { ExampleCard } from "@/features/_example";

export function PlaceholderView({ name }: { name: string }) {
  return (
    <div className="flex h-full items-center justify-center p-12">
      <div className="max-w-md text-center">
        <h2 className="text-base font-semibold">{name}</h2>
        <p className="mt-2 text-sm text-on-surface-muted">
          This view is part of the feature scaffold. The real component will
          land here once the feature is implemented.
        </p>
        <div className="mt-6">
          <ExampleCard />
        </div>
      </div>
    </div>
  );
}
