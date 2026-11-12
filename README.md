# Queue Brew (CS 2114 Project 3, Group 94)

A coffee-shop game. Customers arrive on their own and join a fixed-size, array-based FIFO line. The player makes the front customer's drink by clicking its ingredients into a cup (see the recipe card in the window), then clicks **Complete Order**. A wrong cup is dumped, the customer keeps waiting, and the player is locked out for 3 seconds. If a customer arrives while the line is full, they are turned away and the player loses a life (3 lives). Survive 7 days (60 seconds each) with at least one life to win.

Deliverable 3: first prototype. The model (`Line`, `Customer`, `Order`, `TimeUtil`, `GameState`) is complete and tested without any GUI. The view (`QueueBrewGUI`) and controller (`QueueBrew`) are a thin but playable slice.

## Layout

```
src/queuebrew/
  Order, Customer, Line, TimeUtil   model data and the array-based queue
  Menu                              model: ingredients and each drink's recipe
  GameState                         model: lives, day, clock, arrivals, cup, win/lose
  GameInfo, GameListener            the model/view boundary (read-only getters + one callback)
  ModelDriver                       console driver: plays a full game, no window
  QueueBrew                         controller: owns the 1-second Timer, wires model and view
  QueueBrewGUI                      thin Swing view
test/queuebrew/
  OrderAndCustomerTest, MenuAndOrderTest, LineTest, TimeUtilTest, GameStateTest   JUnit 5, no GUI
```

## How to run

This is an Eclipse project (`.classpath` / `.project` included, JUnit 5 on the classpath). Use *File > Import > Existing Projects into Workspace*.

- **Tests (model only, no view):** right-click the `test` folder > *Run As > JUnit Test*. 68 tests, all passing at hand-in.
- **Model driver (no view):** run `queuebrew.ModelDriver`. It plays a whole 7-day game in the console with a seeded Random: the simulated player adds one correct ingredient per second and completes each order, and wins. Pass the program argument `lazy` for a player who never serves anyone, which loses.
- **How to play:** click ingredient buttons to fill the cup so it matches the front customer's recipe (the recipe card is on the right), then click Complete Order. Clear Cup empties the cup. A wrong cup locks all the buttons for 3 seconds. When a day ends, the game pauses and a popup says you completed the day. Click OK to start the next one.
- **Thin view (the game):** run `queuebrew.QueueBrew`.

From a command line with a JDK, the driver and game do not need JUnit:

```
javac -d bin src/queuebrew/*.java
java -cp bin queuebrew.ModelDriver
java -cp bin queuebrew.QueueBrew
```

## Changes from Spec / Scope to Prototype

This is a running log. Every time the prototype departs from the Scope or the Specification, add a row (newest last) and say why. "Scope" is the Group 94 Scope document; "Spec" is the QueueBrew Specification.

| # | Area | Scope / Spec said | Prototype does | Why |
|---|---|---|---|---|
| 1 | Architecture (Scope to Spec) | Scope: model and view only, view talks to the model. | Spec and prototype add a controller, `QueueBrew`, between them. | Done in the Spec (its section 9). The controller owns the timer and forwards clicks, so the model has no Swing code. |
| 2 | Arrival timing | Spec prose: a customer every 5 s. Constants table, formula and day-2 test: 8 s on day 1. | 8 s on day 1, 1 s faster per day. | The spec contradicted itself. We followed the table, formula and day-2 test. The "tick 5 times, expect one customer" test became "8 ticks". |
| 3 | `Customer.secondsWaiting` | Spec field, read by the patience-timer stretch goal. | Not built. | Nothing reads it yet and the spec gave it no getter. Add it with the patience timer, if we do that. |
| 4 | Validation strictness | `Order`/`Customer` reject null or empty. | Also reject blank (whitespace-only) text. | The spec's own test table hinted at it, and a name of spaces is as bad as an empty one. |
| 5 | Messages | Spec gave two message texts, and never said when a message goes away. | Messages are public constants in `GameState`. `tick()` clears the message at the start of each tick, and a successful action clears it. | Fills a gap, and keeps tests and the view from retyping the text. |
| 6 | `arrivalIntervalForDay` | Spec: private, and "can be static". | `private static`, tested through behavior (8 s on day 1, 7 s on day 2). | Spec had it private. Behavior tests are enough. |
| 7 | Test plan | One normal and one bad-input test per public method. | Tests are grouped by behavior (68 now). Trivial getters are covered inside scenario tests. Added tests the spec did not list (see rows below). | Fewer, more meaningful tests. Every spec'd case is still covered. |
| 8 | Console driver | Not in the spec. | `ModelDriver` plays a whole game with no window. | The "tiny driver" the Deliverable 3 rubric asks for. |
| 9 | **Making the drink** (new) | Scope MVP: one "Complete Order" button that removes the front order. Scope "What we don't know": "How do we complete the order? Choosing buttons that are the components of the coffee?" Spec: `Order` holds only a drink name. | The player adds ingredients to a cup with buttons, then clicks Complete Order. It only serves the front customer if the cup matches that drink's recipe. | Answers that Scope unknown, and makes the game more than clicking one button. |
| 10 | `Menu` class (new) | Not in the spec. | `Menu` holds the ingredient list, each drink's recipe, the recipe card text and random order generation. | Recipes had to live somewhere in the model. It replaces the `DRINKS` array inside `GameState`. |
| 11 | `Order` (new) | `Order(String drink)` and `getDrink()` only. | Adds `getIngredients()` and `matches(cup)`, plus a second constructor `Order(drink, ingredients)`. `Order(drink)` now looks the recipe up in `Menu` and rejects drinks not on the menu. Matching ignores order and checks counts. | Needed to check a cup. |
| 12 | `GameState` (new) | Spec: `completeFrontOrder()` removes the front customer, and fails only on an empty line. | New `addIngredient(String)` and `clearCup()`, plus constants `CUP_CAPACITY` (6) and new messages. `completeFrontOrder()` now also fails on an empty cup (message, customer stays) and on a wrong cup (cup dumped, message, customer stays). | Wrong order was an open question in the Scope ("what are the impacts of this?"). We chose: no life lost, but the cup is dumped, which costs the player time. |
| 13 | `GameInfo` (new) | Eight getters. | Adds `getCup()`, `getIngredients()` and `getRecipeCard()`. | The view needs these to draw buttons and the cup without touching `Menu` or `GameState` internals. |
| 14 | `QueueBrew` and view (new) | One controller method, `onCompleteClicked()`. | Adds `onIngredientClicked(String)` and `onClearCupClicked()`. The view gets ingredient buttons, a Clear Cup button, a cup display and a recipe card. | Wiring for row 9. The controller still has no rules, it only forwards clicks. |
| 15 | Arrival speed floor (new) | Spec: minimum interval 3 s. | Minimum interval 5 s, so arrivals go 8, 7, 6, 5, 5, 5, 5 s by day. | Making a drink takes several clicks, so 3 s arrivals on days 6 and 7 would be impossible. Not yet playtested by a person. |
| 16 | Driver (new) | Driver completed the front order on a timer. | Driver now builds each drink correctly, one ingredient per second. `lazy` argument shows a loss. | Same reason as row 9. |
| 17 | Tests (new) | Spec tests assume `completeFrontOrder()` removes the front customer directly. | A test helper makes the correct cup first. New tests cover the cup, wrong order, empty cup, full cup, unknown ingredient, and `Menu`/`Order` recipes. Spec test cases for tick and the getters are otherwise unchanged. | Follows from rows 9 to 13. |
| 18 | **Wrong-order time penalty** (new) | Scope: "Making the wrong order, what are the impacts of this?" was open. Row 12 first answered it with only a dumped cup. | A wrong cup also locks the player out for `WRONG_ORDER_PENALTY_SECONDS` (3) seconds. During that time `addIngredient`, `clearCup` and `completeFrontOrder` do nothing and return false, and the view disables the buttons and shows a countdown. `GameInfo` gets `getPenaltySecondsLeft()`. The penalty counts down in `tick()`. | A dumped cup alone cost almost nothing, since the player could just rebuild it. A time penalty makes mistakes matter without costing a life. 3 s is a first guess to be playtested. 4 new tests. |
| 19 | **Random arrival gaps** (new) | Scope: orders "arrive on their own at a steady rate". Spec: a fixed interval per day (8 s on day 1, minus 1 s per day). | Each gap is the day's interval plus or minus up to `ARRIVAL_JITTER_SECONDS` (2) seconds, picked after every arrival and at the start of each day, and never below `MIN_ARRIVAL_GAP_SECONDS` (2). Day 1 gaps are 6 to 10 s, and days 5 to 7 are 3 to 7 s. The average is unchanged. New constructor `GameState(Random, int jitter)`. The old constructors use the default jitter, and tests pass 0 for exact timing. | Playtest feedback: the fixed 5 s interval was predictable and a little easy. Bursts of customers make the line harder to plan for. The scope's "steady rate" is deliberately dropped. |
| 20 | **Pause and popup between days** (new) | Scope/Spec: day 1 ends and day 2 starts immediately, with no message. | When a day ends (days 1 to 6), the model enters a paused "between days" state: `tick()` and all cup actions do nothing until `startNextDay()` is called. The view sees `GameInfo.isBetweenDays()` and shows a popup, "Congratulations! You completed day X", with lives left. Clicking OK calls the controller's `onNextDayClicked()`, which calls `startNextDay()`. Day 7 still goes straight to the win message. Customers waiting in line stay put. | Player request. The pause is a model rule (not just a popup) so the clock does not keep running behind a dialog, and so it can be tested without a window. |

## Reflection

### What changed from our specification?

The classes and method signatures in the spec were built as written, and then extended. See the **Changes from Spec / Scope to Prototype** table above for the full list with reasons. The short version:

- The spec contradicted itself on the arrival interval (5 s in prose, 8 s in the table, formula and tests). We followed the table.
- `Customer.secondsWaiting` was dropped until a feature needs it, and validation became stricter (blank text is rejected).
- The biggest change is the **cup**: the Scope listed "how do we complete the order?" as an unknown, and we answered it. Orders now have recipes (`Menu`, `Order`), `GameState` tracks a cup, and Complete Order checks it. This changed `completeFrontOrder()`, `GameInfo`, the controller and the view.
- Tests are grouped by behavior instead of one normal and one bad-input test per method, and `ModelDriver` was added.
- **Validation lives where the spec said.** Rules live in the model (`Line`, `GameState`, `Order`, `Customer`, `Menu`). The view only displays `getMessage()`. The controller does not validate anything. The cup is checked in `GameState`, not in the GUI.

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
| Arrival rate, and whether it should change per day | **Provisional, playtested once:** the average gap is 8 s on day 1, 1 s shorter each day, minimum 5 s (raised from 3 s because making a drink takes several clicks). Each gap now varies by up to 2 s (change-log row 19). The player found the fixed 5 s interval fine but a bit easy, so randomizing it is the first attempt at more difficulty. More playtesting is needed. |
| Day length / game length for a demo | **Open:** `SECONDS_PER_DAY` is a single constant, so it is easy to change. Not yet decided. |
| Manual "add order" form | **Dropped:** automatic arrivals plus the seeded driver cover testing, so the form is not needed. |
| Score | **Open:** win/lose only for now. |
| Patience timers while keeping FIFO | **Open:** not started. The idea is to check only the front customer, since removing from the middle breaks FIFO. |
| How to complete an order (coffee-component buttons?) and what a wrong order does | **Answered:** the player clicks ingredient buttons to fill a cup, then Complete Order checks it against the front customer's recipe. A wrong cup is dumped and the customer keeps waiting, with no life lost. The penalty is now a 3-second lockout (see change-log row 18); whether it is the right size is open until playtested. |

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
| `Order` | **Yes (model)** | Add `getPrepSeconds()`; each drink's prep time would come from `Menu`, so `Menu` changes too. |
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
| `Menu.java` | Veronica | Added with the cup feature; GenAI-assisted |
| `MenuAndOrderTest.java` | Veronica | Tested; GenAI-assisted |
| `LineTest.java` | Nandini | Tested; GenAI-assisted |
| `TimeUtilTest.java` | Nandini | Tested; GenAI-assisted |
| `GameStateTest.java` | Veronica | Tested; GenAI-assisted |
| `README.md` | JC | GenAI-assisted; reviewed by team |

### GenAI note

We gave Claude our Scope and Specification (class design, method signatures, validation table and test plan) and the Deliverable 3 requirements, and asked it to implement the model, a thin Swing view and controller, JUnit tests, a console driver and the README. We kept the spec's class design and method signatures as written. Claude found that the spec said both "every 5 seconds" and "8 seconds" for arrivals, and followed the constants table. It also removed the unused `secondsWaiting` field, and we kept that change. It ran the 38 tests (all pass), the driver, and launched the GUI for a few seconds without errors. We later asked Claude to add the step of making the drink before completing it, and to keep the "Changes from Spec / Scope to Prototype" table up to date. It added `Menu`, the cup logic and tests, raised the minimum arrival interval from 3 s to 5 s, and its new tests caught a null-handling bug (`Menu.isIngredient(null)` threw a NullPointerException instead of the intended IllegalArgumentException), which it fixed. Then, at our request, it added a 3-second lockout for wrong orders, random gaps between arrivals, and a pause with a "you completed day X" popup between days. It checked the popup with a throwaway script that ticked the game through a day and clicked OK. 68 tests pass. [TEAM: add what you personally reviewed or changed, what you asked Claude about the change feature, and anything you rejected.]
