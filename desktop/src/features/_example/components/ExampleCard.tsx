import { type FormEvent, useState } from "react";

import { Button, Card, Input, Separator } from "@/components/ui";
import { useExampleStore } from "../stores/exampleStore";
import { useExampleCount } from "../hooks/useExampleCount";
import { sortByCreatedAt } from "../lib/ids";

/**
 * Fully-implemented reference component for the `_example` feature.
 * Drop this anywhere to demonstrate the wiring end-to-end.
 */
export function ExampleCard() {
  const items = useExampleStore((s) => s.items);
  const add = useExampleStore((s) => s.add);
  const remove = useExampleStore((s) => s.remove);
  const reset = useExampleStore((s) => s.reset);
  const count = useExampleCount();

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = title.trim();
    if (!trimmed) return;
    add(trimmed, description.trim() || undefined);
    setTitle("");
    setDescription("");
  };

  const sorted = sortByCreatedAt(items);

  return (
    <Card
      title="Example feature"
      subtitle={`${count} item${count === 1 ? "" : "s"} — fully implemented reference`}
      className="max-w-xl"
    >
      <form onSubmit={submit} className="flex flex-col gap-2">
        <Input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Title"
          aria-label="Example title"
        />
        <Input
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Description (optional)"
          aria-label="Example description"
        />
        <div className="flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={reset} disabled={count === 0}>
            Reset
          </Button>
          <Button type="submit" variant="primary" disabled={title.trim().length === 0}>
            Add
          </Button>
        </div>
      </form>
      <Separator className="my-4" />
      <ul className="flex flex-col gap-2">
        {sorted.length === 0 ? (
          <li className="text-sm text-on-surface-muted">No items yet.</li>
        ) : (
          sorted.map((it) => (
            <li
              key={it.id}
              className="flex items-start justify-between gap-3 rounded-md bg-surface-sunken p-3"
            >
              <div className="min-w-0">
                <div className="text-sm font-medium">{it.title}</div>
                {it.description && (
                  <div className="text-xs text-on-surface-muted mt-0.5">
                    {it.description}
                  </div>
                )}
              </div>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => remove(it.id)}
                aria-label={`Remove ${it.title}`}
              >
                Remove
              </Button>
            </li>
          ))
        )}
      </ul>
    </Card>
  );
}
