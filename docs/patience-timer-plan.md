# Patience timer: front customer walks out (Prototype 2 plan)

Status: **planned, not implemented.** A teammate is implementing it as part of Prototype 2 (Deliverable 4). Nothing in `src/` mentions patience yet, and the game behaves as at commit `050ea57`.

## Context
The scope's stretch goal was a patience timer, but it was blocked on "how do we do this and keep FIFO?" (README unknowns table; change-log row 3 deferred `Customer.secondsWaiting`). The team's answer: only the customer **at the front of the line** can run out of patience and walk out, so nobody is ever removed from the middle and FIFO holds.

## Recommended design (defaults the implementer can change)
- **Whose clock:** only the front customer's patience counts down. It starts at full when they become the front (after the previous front is served or walks out) and resets for the next one. Customers behind the front do not wait on a timer.
- **Length:** a fixed `PATIENCE_SECONDS = 15` constant in `GameState`. Easy to tune after playtesting.
- **Cost:** when it hits 0, the front customer leaves, the player loses 1 life (same as turning someone away), and the cup is dumped. Message: "<name> got tired of waiting and left: -1 life."
- **Pauses:** the countdown does not run between days or after the game ends. The wrong-order lockout should **keep** the clock running, otherwise a wrong order would also freeze patience and weaken the penalty.
- **End of day:** the front timer resets with the line when the line is cleared, so the next day starts fresh.

## Changes (prediction to compare against in the Prototype 2 reflection)
Model (no Swing):
- `GameState.java`: new field `frontPatienceLeft`, constant `PATIENCE_SECONDS`, new message. In `tick()`: if the line is non-empty, decrement; at 0 call a new private `frontWalksOut()` (dequeue, clear cup, lose a life, set LOST at 0 lives). Reset the counter whenever the front changes: in `completeFrontOrder()` on success, in `addArrival()` when the line was empty, in `endDay()` when the line is cleared, and after a walk-out.
- `GameInfo.java`: add `int getFrontPatienceLeft()` (0 when the line is empty) so the view can show it without touching internals.
- **Should not change:** `Line` (uses existing `peek()` and `dequeue()`), `Customer`, `Order`, `Menu`.

View / controller:
- `QueueBrewGUI.java`: show "Front customer patience: Ns", read from `GameInfo`.
- **Should not change:** `QueueBrew.java`, because the player takes no new action.

## Tests (`GameStateTest.java`, no GUI)
- Front customer walks out after exactly 15 ticks when not served; life lost; message set; line shortens by one.
- Customers behind the front never leave from the middle: with 3 waiting, only the first leaves, and the second then becomes front with a fresh 15 s.
- Serving the front resets the clock for the next customer.
- Timer starts when the first customer arrives into an empty line, and does not run on an empty line.
- Wrong-order lockout does not freeze the patience clock.
- Day end, game over and the between-days pause: patience does not tick or fire; `getFrontPatienceLeft()` is 0 for an empty line.
- A walk-out that takes the last life sets status LOST.
- **Watch out:** many existing tests tick for a long time without serving anyone (for example the fill-the-line and turned-away tests). Those customers will now walk out after 15 s. Those tests need to serve, or the timer needs a way to be switched off in tests (for example an extra constructor parameter, like the existing jitter parameter on `GameState(Random, int)`).

## README
Add a row to "Prototype 2 changes", mark the "Patience timers while keeping FIFO" unknown as answered, fix change-log row 3, update the test count, and fill in the Prototype 2 Reflection by comparing the classes actually touched with the prediction above.

## Verification
Run the JUnit tests and `ModelDriver`, launch `QueueBrew`, and check the countdown shows and an ignored front customer leaves after about 15 s. Then playtest: with 15 s patience, 5 s gaps, the 3-second lockout and life loss for leftover customers, check the game is still winnable.

## Open design questions (undecided)
- Is a life lost per walk-out right, or would a time penalty or no cost be better?
- Is a fixed 15 s right, or should it depend on the recipe size or the day?
- Should the wrong-order lockout keep the patience clock running (recommended)?
