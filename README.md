# Queue Brew (CS 2114 Project 3, Group 94)

A coffee-shop game. Customers arrive on their own and join a fixed-size, array-based FIFO line. The player serves the front order with **Complete Order**. If a customer arrives while the line is full, they are turned away and the player loses a life (3 lives). Survive 7 days (60 seconds each) with at least one life to win.

Deliverable 3: first prototype. The model (`Line`, `Customer`, `Order`, `TimeUtil`, `GameState`) is complete and tested without any GUI. The view (`QueueBrewGUI`) and controller (`QueueBrew`) are a thin but playable slice.

## Layout

```
src/queuebrew/
  Order, Customer, Line, TimeUtil   model data and the array-based queue
  GameState                         model: lives, day, clock, arrivals, win/lose
  GameInfo, GameListener            the model/view boundary (read-only getters + one callback)
  ModelDriver                       console driver: plays a full game, no window
  QueueBrew                         controller: owns the 1-second Timer, wires model and view
  QueueBrewGUI                      thin Swing view
test/queuebrew/
  OrderAndCustomerTest, LineTest, TimeUtilTest, GameStateTest   JUnit 5, no GUI
```

## How to run

This is an Eclipse project (`.classpath` / `.project` included, JUnit 5 on the classpath). Use *File > Import > Existing Projects into Workspace*.

- **Tests (model only, no view):** right-click the `test` folder > *Run As > JUnit Test*. 38 tests, all passing at hand-in.
- **Model driver (no view):** run `queuebrew.ModelDriver`. It plays a whole 7-day game in the console with a seeded Random. Pass a program argument for how many seconds the simulated player waits between completions: default `2` ends in a win, `100` ends in a loss.
- **Thin view (the game):** run `queuebrew.QueueBrew`.

From a command line with a JDK, the driver and game do not need JUnit:

```
javac -d bin src/queuebrew/*.java
java -cp bin queuebrew.ModelDriver
java -cp bin queuebrew.QueueBrew
```

## Reflection

### What changed from our specification?

The classes and method signatures in the spec were built as written. The differences, and why:

- **Arrival timing: the spec contradicted itself.** Section 4 said "one new customer every 5 seconds" in prose, but its constants table, the `arrivalIntervalForDay` formula and the day-2 test all assume 8 seconds on day 1 (down 1 second per day, minimum 3). The test plan also said "tick 5 times, expect one customer". We followed the table and formula (8 s), so that test became "7 ticks: nobody; 8th tick: one customer" (`firstCustomerArrivesAfterEightSeconds`).
- **`Customer.secondsWaiting` was not built.** The spec listed the field, but nothing reads it until the patience-timer stretch goal, and it had no getter or setter. We will add it with that feature, if we attempt it.
- **Stricter validation.** `Order` and `Customer` reject blank (whitespace-only) text as well as null and empty, using `isBlank()`. The spec's test table itself hints at this ("whitespace-only text if such input is rejected").
- **Message handling filled in a gap.** The spec never said when a message goes away. `tick()` clears the message at the start of each tick, and a successful `completeFrontOrder()` clears it. The empty-line and turned-away messages are public constants in `GameState` so tests and the view do not retype them.
- **`GameState.arrivalIntervalForDay` is `private static`.** We test it through behavior (day 1 arrives at 8 s, day 2 at 7 s) instead of calling it directly.
- **Tests are grouped differently.** The spec had one normal and one bad-input test per method. We wrote 38 tests grouped by behavior. Trivial getters are covered inside the scenario tests (for example `getQueueCapacity` inside `queueSizeNeverExceedsCapacity`). We added a few the spec did not list: FIFO order after dequeue-then-enqueue on a full line, message clearing, and same-seed repeatability.
- **Added `ModelDriver`**, a console driver that plays a complete game with no window. It is not in the spec. It is the "tiny driver" the rubric asks for.
- **Validation lives where the spec said.** Rules live in the model (`Line`, `GameState`, the `Order`/`Customer` constructors). The view only displays `getMessage()`. The controller does not validate anything.

### Did the model really stand alone?

Yes. Nothing in the five model classes imports Swing or `java.awt`, and `GameState` never references `QueueBrew` or `QueueBrewGUI`. All the tests and `ModelDriver` play full games using only the model. Its one outward call is `GameListener.gameChanged()`. Nothing had to move across the line to make it run. Where the line is still not clean:

- `GameInfo.getStatus()` returns `GameState.Status`, so the read-only interface depends on the class that implements it. Moving `Status` into its own top-level enum would fix this, and the view would then stop importing a model class.
- The view also reads constants from `GameState` (`GameState.TOTAL_DAYS` for the "Day x of 7" label) instead of getting them from `GameInfo`. We should add `getTotalDays()` to `GameInfo`.
- The wording of the win/lose text is in the view, but the wording of the empty-line and turned-away messages is in the model. That matches the spec (the model writes gameplay messages) but is worth defending, since it is a judgement call.

### What happened to our unknowns?

| Unknown from the scope | Status |
|---|---|
| Language and GUI toolkit | **Answered:** Java, Swing (`JFrame`, `JList`, `javax.swing.Timer`). |
| Implementing automatic arrivals and the day countdown | **Answered:** the controller's 1-second `Timer` calls `GameState.tick()`. The model counts seconds itself, so no real clock is needed to test it. |
| How to test this kind of game | **Answered for the model:** inject a seeded `Random`, call `tick()` in a loop, assert on state. No GUI is needed. **Open for the view:** we have no automated GUI tests. |
| Queue capacity | **Provisional:** 6. Not yet playtested by a person. |
| Arrival rate, and whether it should change per day | **Provisional:** 8 s on day 1, 1 s faster each day, minimum 3 s. In the driver, a player who serves every 6 s survives until day 6 and then loses, so days 6 and 7 (3 to 4 s arrivals) may be too hard. Needs playtesting. |
| Day length / game length for a demo | **Open:** `SECONDS_PER_DAY` is a single constant, so it is easy to change. Not yet decided. |
| Manual "add order" form | **Dropped:** automatic arrivals plus the seeded driver cover testing, so the form is not needed. |
| Score | **Open:** win/lose only for now. |
| Patience timers while keeping FIFO | **Open:** not started. The idea is to check only the front customer, since removing from the middle breaks FIFO. |
| How to complete an order (coffee-component buttons?) and what a wrong order does | **Open:** the MVP uses one Complete Order button, so a wrong order cannot happen yet. |

### What doesn't work yet?

- No automated tests for `QueueBrew` or `QueueBrewGUI`. The GUI launched and ran for several seconds without an exception, but nobody has played a full game in it yet.
- There is no score, and no way to restart after a win or loss (the window must be closed and reopened).
- The turned-away message is only visible for one second, because the next tick clears it.
- The game balance (capacity, arrival speed on days 6 and 7) has not been checked by a human player.
- No spec'd tests are failing. The only intentional gaps are the GUI tests above.

## Next Steps (Deliverable 4)

### Finishing the MVP, in order

1. **Playtest and tune** the constants (capacity, arrival interval, day length) with a human at the keyboard, and record what we chose and why.
2. **Fix the boundary leaks** above: move `Status` out of `GameState`, add `getTotalDays()` to `GameInfo`.
3. **Polish the view:** keep the turned-away message visible longer, show the "next order" clearly, and add a Play Again button (through the controller).
4. **GUI smoke test:** one scripted check that builds `QueueBrewGUI` against a `GameState` and calls `gameChanged()` through a full game.

### Our change feature: preparation times

Different drinks take different amounts of time to make, so a black coffee is quick and a Vanilla Caramel Latte is slow. Clicking Complete Order starts preparing the front customer, and the order only leaves the line when its preparation seconds have run out. The line moves at a different speed depending on what is at the front. This changes model state and logic, and the player reaches it through the existing button and a progress display.

Prediction of what it will touch:

| Class | Change? | Why |
|---|---|---|
| `Order` | **Yes (model)** | Add `getPrepSeconds()`; a constructor taking prep time (the old one gets a default). |
| `GameState` | **Yes (model)** | New state: the order being prepared and seconds left. `tick()` counts it down and finishes the order. `completeFrontOrder()` now means "start preparing". It needs rules for clicking while already preparing. |
| `GameInfo` | **Yes (boundary)** | One new getter for the prep seconds left, so the view can show it. |
| `QueueBrewGUI` | **Yes (view)** | Display the progress and disable the button while preparing. |
| `Line`, `Customer`, `TimeUtil`, `GameListener` | **No** | The queue still only enqueues, dequeues and peeks. Nothing about FIFO changes. |
| `QueueBrew` | **Should not** | `onCompleteClicked()` still just forwards the click to the model. If it has to change, that is a boundary problem we should report. |

*GenAI tip from the assignment: check this list with GenAI before committing.* [TEAM: ask Claude which classes the feature forces you to edit, compare to this table, and note what you asked in the GenAI note below.]

### Stretch goals

- **Committing to:** preparation times (above, as the change feature).
- **Already done:** rising difficulty. Arrivals speed up each day, implemented in `GameState` and tested.
- **Dropping:** the food queue (a second queue is a lot of extra UI and model state) and patience timers (we do not have a good answer for FIFO yet). We can revisit patience timers if there is time after the change feature.

### New tests needed

- `Order`: prep seconds stored; rejects zero or negative prep time (bad input).
- `GameState`: clicking starts prep and the order stays in line; the order leaves exactly when the prep seconds end; clicking while preparing changes nothing and gives a message; clicking on an empty line still gives the empty-line message; no prep progress after the game ends; the line cannot lose its front customer mid-prep.
- `Line`: no new tests, since it is unchanged. That is part of the prediction.

### Who does what

Roles follow the spec's Division of Work. [TEAM: confirm each owner before submitting.]

| Item | Owner |
|---|---|
| Playtest and tune constants | Everyone, recorded by Veronica |
| Boundary fixes (`Status`, `getTotalDays`) | Nandini |
| Preparation times in the model (`Order`, `GameState`, `GameInfo`) and their tests | Veronica, Nandini |
| View changes (progress display, Play Again, message timing) | Shea |
| Controller (restart wiring) and GUI smoke test | JC |
| Reflection and README update for Deliverable 4 | JC |

## Attribution

[TEAM: this table was drafted from the spec's Division of Work and must be corrected to reflect who actually did what before submitting. Every member needs a substantive contribution to at least one class or test file.]

| File | Who | How |
|---|---|---|
| `Order.java` | Veronica | Wrote from the spec; GenAI-assisted (Claude drafted first version) |
| `Customer.java` | Veronica | Wrote from the spec; GenAI-assisted |
| `Line.java` | Nandini | Wrote from the spec; GenAI-assisted |
| `TimeUtil.java` | Nandini | Wrote from the spec; GenAI-assisted |
| `GameInfo.java`, `GameListener.java` | JC | Wrote from the spec's boundary section; GenAI-assisted |
| `GameState.java` | Veronica, Nandini | Wrote and revised; GenAI-assisted |
| `ModelDriver.java` | JC | GenAI-assisted (not in spec) |
| `QueueBrew.java` | JC | Wrote controller; GenAI-assisted |
| `QueueBrewGUI.java` | Shea | Wrote thin view; GenAI-assisted |
| `OrderAndCustomerTest.java` | Veronica | Tested; GenAI-assisted |
| `LineTest.java` | Nandini | Tested; GenAI-assisted |
| `TimeUtilTest.java` | Nandini | Tested; GenAI-assisted |
| `GameStateTest.java` | Veronica | Tested; GenAI-assisted |
| `README.md` | JC | GenAI-assisted; reviewed by team |

### GenAI note

We gave Claude our Scope and Specification (class design, method signatures, validation table and test plan) and the Deliverable 3 requirements, and asked it to implement the model, a thin Swing view and controller, JUnit tests, a console driver and the README. We kept the spec's class design and method signatures as written. Claude found that the spec said both "every 5 seconds" and "8 seconds" for arrivals, and followed the constants table. It also removed the unused `secondsWaiting` field, and we kept that change. It ran the 38 tests (all pass), the driver, and launched the GUI for a few seconds without errors. [TEAM: add what you personally reviewed or changed, what you asked Claude about the change feature, and anything you rejected.]
