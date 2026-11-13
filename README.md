# Queue Brew (CS 2114 Project 3, Group 94)

A coffee-shop game. Customers arrive on their own and join a fixed-size, array-based FIFO line. The player makes the front customer's drink by clicking its ingredients into a cup (see the recipe card in the window), then clicks **Complete Order**. A wrong cup is dumped, the customer keeps waiting, and the player is locked out for 3 seconds. If a customer arrives while the line is full, they are turned away and the player loses a life (3 lives). Survive 7 days (60 seconds each) with at least one life to win.

Status: **Prototype 2 (Deliverable 4) is in progress.** Deliverable 3 (Prototype 1) is described by the older sections below, which are kept as history. The patience-timer plan for Prototype 2 is in [docs/patience-timer-plan.md](docs/patience-timer-plan.md).

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

- **Tests (model only, no view):** right-click the `test` folder > *Run As > JUnit Test*. 81 tests, all passing at hand-in.
- **Model driver (no view):** run `queuebrew.ModelDriver`. It plays a whole 7-day game in the console with a seeded Random: the simulated player makes each drink correctly and completes it, at 3 clicks per second by default (it wins). Pass a number to change the clicks per second (1 loses on day 6, 2 or more wins) or `lazy` for a player who never serves anyone, which loses. The robot never misclicks, so a human is slower than it.
- **How to play:** click ingredient buttons to fill the cup so it matches the front customer's recipe (the recipe card is on the right), then click Complete Order. Clear Cup empties the cup. A wrong cup locks all the buttons for 3 seconds. When a day ends, the game pauses and a popup says you completed the day. Click OK to start the next one. The line is empty at the start of each day, but each customer still in line when a day ends costs a life.
- **Thin view (the game):** run `queuebrew.QueueBrew`.

From a command line with a JDK, the driver and game do not need JUnit:

```
javac -d bin src/queuebrew/*.java
java -cp bin queuebrew.ModelDriver
java -cp bin queuebrew.QueueBrew
```

## Changes from Spec / Scope to Prototype

**Prototype 1 changes are rows 1 to 22 below** (everything up to commit `050ea57`). **Every change from here on goes in the "Prototype 2 changes" table that follows it.**

This is a running log. Every time the prototype departs from the Scope or the Specification, add a row (newest last) and say why. "Scope" is the Group 94 Scope document; "Spec" is the QueueBrew Specification.

| # | Area | Scope / Spec said | Prototype does | Why |
|---|---|---|---|---|
| 1 | Architecture (Scope to Spec) | Scope: model and view only, view talks to the model. | Spec and prototype add a controller, `QueueBrew`, between them. | Done in the Spec (its section 9). The controller owns the timer and forwards clicks, so the model has no Swing code. |
| 2 | Arrival timing | Spec prose: a customer every 5 s. Constants table, formula and day-2 test: 8 s on day 1. | 8 s on day 1, 1 s faster per day. | The spec contradicted itself. We followed the table, formula and day-2 test. The "tick 5 times, expect one customer" test became "8 ticks". |
| 3 | `Customer.secondsWaiting` | Spec field, read by the patience-timer stretch goal. | Not built. | Nothing reads it yet and the spec gave it no getter. Add it with the patience timer, if we do that. |
| 4 | Validation strictness | `Order`/`Customer` reject null or empty. | Also reject blank (whitespace-only) text. | The spec's own test table hinted at it, and a name of spaces is as bad as an empty one. |
| 5 | Messages | Spec gave two message texts, and never said when a message goes away. | Messages are public constants in `GameState`. `tick()` clears the message at the start of each tick, and a successful action clears it. | Fills a gap, and keeps tests and the view from retyping the text. |
| 6 | `arrivalIntervalForDay` | Spec: private, and "can be static". | `private static`, tested through behavior (8 s on day 1, 7 s on day 2). | Spec had it private. Behavior tests are enough. |
| 7 | Test plan | One normal and one bad-input test per public method. | Tests are grouped by behavior (81 now). Trivial getters are covered inside scenario tests. Added tests the spec did not list (see rows below). | Fewer, more meaningful tests. Every spec'd case is still covered. |
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
| 20 | **Pause and popup between days** (new) | Scope/Spec: day 1 ends and day 2 starts immediately, with no message. | When a day ends (days 1 to 6), the model enters a paused "between days" state: `tick()` and all cup actions do nothing until `startNextDay()` is called. The view sees `GameInfo.isBetweenDays()` and shows a popup, "Congratulations! You completed day X", with lives left. Clicking OK calls the controller's `onNextDayClicked()`, which calls `startNextDay()`. Day 7 still goes straight to the win message. (What happens to the line is row 21.) | Player request. The pause is a model rule (not just a popup) so the clock does not keep running behind a dialog, and so it can be tested without a window. |
| 21 | **Line clears at the end of each day** (new) | Spec: nothing said about customers still in line when a day ends, so they carried over into the next day. | At the end of days 1 to 6 the line, the cup and any wrong-order penalty are all cleared. The next day starts with an empty line. (Whether leftover customers cost lives is row 22.) New method `Line.clear()` (the spec's `Line` had none), and the popup says the line clears. | Playtest feedback: customers carrying over made later days snowball. A fresh line each day makes each day its own round. |
| 22 | **Lives lost for customers left in line at closing** (new) | Spec: lives are lost only when a customer arrives to a full line. | When any day ends (including day 7), the player loses one life per customer still in line (never below 0). If that reaches 0 lives the game is LOST, even on day 7. The model writes the message ("2 customers were still in line at closing: -2 lives."), and the end-of-day popup shows it. Also, no customer arrives in the last 3 seconds of a day (`CLOSING_SECONDS_WITHOUT_ARRIVALS`; `tick()` skips arrivals once 2 or fewer seconds are left). | Player request: otherwise the line clearing (row 21) let the player ignore the queue at the end of a day. The quiet period at closing is our addition (first one second, then widened to 3 seconds at the player's request). Without it, days 3, 5, 6 and 7 (gaps of 6 s or 5 s that divide 60) would always drop a customer on the closing second that nobody could serve. Not in the scope or spec. 7 new tests. |

### Prototype 2 changes (from here on)

Same columns as above. Add a row for every change made after `050ea57`. Rows are numbered from 23 so they continue the list above.

| # | Area | Scope / Spec said | Prototype 2 does | Why |
|---|---|---|---|---|
| 23 | **Patience timer** (planned, not built yet) | Scope stretch goal: a timer per order, and customers who wait past a limit walk out, "while maintaining FIFO". | Plan: only the front customer's patience counts down, and when it runs out they leave and cost a life. Full plan, predicted class changes and tests: [docs/patience-timer-plan.md](docs/patience-timer-plan.md). A teammate is implementing it. | The scope left "how do we keep FIFO?" open. Timing only the front customer means nobody is removed from the middle. Replaces the D3 plan's change feature (preparation times), see the Prototype 2 reflection. |
| 24 | **Harder arrivals toward the final day** (Prototype 2) | Rows 15 and 19: average gap 8 s on day 1, down 1 s per day to a floor of 5 s, with each gap varying by up to 2 s. The game was too easy. | The average gap now shrinks evenly from 8 s on day 1 to `FINAL_DAY_ARRIVAL_INTERVAL_SECONDS` (3 s) on day 7: 8, 7, 6, 5, 5, 4, 3 s by day. The random variation shrinks on later days (about a third of the interval, at least 1 s), so day 7 gaps are **2 to 4 seconds**. Constants `ARRIVAL_INTERVAL_DECREASE_PER_DAY` and `MIN_ARRIVAL_INTERVAL_SECONDS` are removed. `ModelDriver` now takes a clicks-per-second argument (default 3). | Playtest feedback: the game was way too easy. Rows 15 and 19 are superseded for days 4 to 7 (days 1 to 3 are unchanged). A perfect robot at 1 click per second now loses on day 6 and one at 2 or more wins, so it is winnable but tight. Not yet playtested by a person. 2 new tests. |

## Changes between prototypes

What is different between the **Deliverable 3 hand-in** (commit `7b1a2e5`, "Prototype 1 as handed in") and the code now. The detailed reasons for each change are in the change-log tables above (row numbers in the last column). *[TEAM: the change-log's "Prototype 1" rows 1 to 22 run up to commit `050ea57`, so rows 9 to 22 were made after the D3 hand-in. This table lists everything by commit so it reads correctly either way. Decide which prototype you count those rows under in the Deliverable 4 reflection.]*

### By commit

| Commit | What changed for the player | Change-log rows | Tests |
|---|---|---|---|
| `7b1a2e5` (D3 hand-in) | One **Complete Order** button removes the front customer. Customers arrive every 8 s on day 1, 1 s faster each day. Seven 60-second days, three lives. | 1 to 8 | 38 |
| `7519ee0` | Players now **make the drink**: click ingredients into a cup (recipe card shown), then Complete Order. A wrong cup is dumped and locks the buttons for 3 s. Arrival floor raised from 3 s to 5 s. | 9 to 18 | 60 |
| `91771fb` | **Random arrival gaps** (plus or minus 2 s). A **"you completed day X" popup** pauses the game between days. | 19, 20 | 68 |
| `0b9e2d6` | The **line clears at the end of each day**, and each customer left in line costs a **life** (day 7 included). | 21, 22 | 78 |
| `050ea57` | No customer arrives in the **last 3 seconds** of a day. | 22 | 79 |
| `e429481` (Prototype 2 begins) | **Harder arrivals**: the average gap shrinks to 3 s on day 7, with gaps of 2 to 4 s. Prototype 2 README and the patience-timer plan added. | 24 (and 23 planned) | 81 |

### At a glance: D3 hand-in versus now

| Part | D3 hand-in | Now |
|---|---|---|
| Model classes | `Order`, `Customer`, `Line`, `TimeUtil`, `GameState` | Adds `Menu`. `Order` gains `getIngredients()`, `matches()` and a recipe constructor. `Line` gains `clear()`. |
| `GameState` | `tick()`, `completeFrontOrder()`, `addListener()` | Adds `addIngredient()`, `clearCup()`, `startNextDay()` and a constructor taking an arrival jitter. New rules: cup matching, wrong-order lockout, random arrival gaps that shrink toward day 7, pause between days, line clears at closing, lives lost for leftover customers, quiet last 3 seconds. |
| `GameInfo` (what the view can read) | 8 getters | 13: adds `getCup()`, `getIngredients()`, `getRecipeCard()`, `getPenaltySecondsLeft()` and `isBetweenDays()`. |
| Controller (`QueueBrew`) | `onCompleteClicked()` | Adds `onIngredientClicked()`, `onClearCupClicked()` and `onNextDayClicked()`. Still no game rules in it. |
| View (`QueueBrewGUI`) | Labels, line list, one button | Adds ingredient buttons, Clear Cup, a cup display, a recipe card, the lockout countdown, and the end-of-day popup. Still keeps no game state of its own. |
| `ModelDriver` | Plays a game by completing orders on a timer | Builds each drink and takes a clicks-per-second argument (default 3). |
| Tests | 38, in 4 files | 81, in 5 files (adds `MenuAndOrderTest`). |
| Still not built | Patience timer, food queue, preparation times | Patience timer is planned for Prototype 2 ([plan](docs/patience-timer-plan.md)). Food queue and preparation times are dropped. |

### What stayed the same

- `Line` is still the only queue, with the same `enqueue`, `dequeue`, `peek`, `isEmpty`, `getSize`, `getCapacity` and `toArray` behaviour, so FIFO order never changed.
- The model still has no Swing imports. Everything above was tested without a window.

## Prototype 1 Reflection (Deliverable 3)

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
| Arrival rate, and whether it should change per day | **Provisional, playtested once (Prototype 2 row 24 makes later days faster, 2 to 4 s on day 7):** the average gap is 8 s on day 1, 1 s shorter each day, minimum 5 s (raised from 3 s because making a drink takes several clicks). Each gap now varies by up to 2 s (change-log row 19). The player found the fixed 5 s interval fine but a bit easy, so randomizing it is the first attempt at more difficulty. More playtesting is needed. |
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

## Prototype 1 Next Steps (written for Deliverable 4)

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

## Reflection

*Prototype 2 (Deliverable 4). Fill in the starred items after the patience timer is built. The prediction to compare against is in [docs/patience-timer-plan.md](docs/patience-timer-plan.md).*

### How did the design absorb the change?

- **Predicted (before building):** the timer should touch `GameState` (new countdown state and walk-out rule) and `GameInfo` (one new getter) in the model, and `QueueBrewGUI` in the view. It should **not** touch `Line`, `Customer`, `Order`, `Menu` or the controller `QueueBrew`.
- **Actual:** *[TEAM: list the classes the timer really touched, and say what matched and what surprised you.]*

### Did the change cross the model/view line?

*[TEAM: did the view have to learn anything new beyond `GameInfo.getFrontPatienceLeft()`? Did the rule leak into the view or controller?]*

### What changed from our Deliverable 3 plan?

- **The change feature switched.** The Prototype 1 Next Steps named *preparation times* as the Deliverable 4 change feature. We are instead using the *patience timer*, because the scope already listed it as a stretch goal and we had settled how to keep FIFO (only the front customer can walk out). *[TEAM: confirm, and decide how to describe the work done between D3 and now, listed in change-log rows 9 to 22: the drink-making cup, the wrong-order lockout, random arrival gaps, the day-end popup, clearing the line each day, losing lives for customers left at closing, and the quiet last 3 seconds.]*
- Preparation times are not being built. *[TEAM: confirm, and move it to the dropped stretch goals below.]*

### What doesn't work yet?

- *[TEAM: update after the timer lands.]*
- Known from Prototype 1: no automated GUI tests, no score, no Play Again button, and game balance (arrival gaps, patience, lockout, life loss for leftover customers) is only lightly playtested.
- The `GameInfo` boundary leaks listed in the Prototype 1 reflection (`Status` lives inside `GameState`, the view reads `GameState.TOTAL_DAYS`) are still open.

### What design change would make the next feature cheaper?

- Move `Status` into its own top-level enum and add `getTotalDays()` to `GameInfo`, so the view stops importing model internals. *[TEAM: add anything the timer work showed you, for example collecting the per-day tuning constants in one place.]*

## Next Steps

*Prototype 2 (Deliverable 4). Owners are placeholders until the team assigns them.*

### What is left for the final build, in order

1. **Patience timer** in the model, view and tests (see the plan). Owner: *[teammate]*.
2. **Fix the existing tests** that tick for a long time without serving, so they survive the walk-out rule (see the plan's "Watch out").
3. **Fix the boundary leaks** (`Status`, `getTotalDays`).
4. **Play Again button** through the controller, and show the turned-away and walk-out messages for longer than one second.
5. **Playtest and tune** the constants together: arrival gaps, patience length, lockout length, life loss for leftover customers.
6. **GUI smoke test**: one scripted check that builds `QueueBrewGUI` against a `GameState` and plays a day.
7. **Finish the README**: fill in the Prototype 2 reflection, the attribution table and the GenAI note.

### Stretch goals

- **Committing to:** patience timer (the change feature).
- **Already done:** rising difficulty (arrivals speed up each day).
- **Dropping:** preparation times (replaced as the change feature), and the food queue (too much extra model and view state). *[TEAM: confirm.]*

### 3-minute demo (draft)

| Time | What we show |
|---|---|
| 0:00 to 0:30 | One slide or sentence on the design: array-based `Line`, model / controller / view split, and that `GameState` runs with no window (run `ModelDriver`). |
| 0:30 to 1:30 | Play day 1: customers arrive at random gaps, build a drink from the recipe card, complete it. Make one wrong order to show the 3-second lockout. |
| 1:30 to 2:15 | The new feature: leave the front customer waiting and let them walk out, losing a life, while the customers behind keep their place. Then the end-of-day popup and the line clearing. |
| 2:15 to 3:00 | Run the JUnit tests and show they pass, pointing at the interleaved enqueue/dequeue FIFO test and the patience tests. |

*[TEAM: time this with a stopwatch and trim if it runs over.]*

### Who does what

| Item | Owner |
|---|---|
| Patience timer: model, `GameInfo` getter, tests | *[teammate]* |
| Patience countdown in the view | Shea *[confirm]* |
| Fix existing tests for the walk-out rule | *[teammate]* |
| Boundary fixes (`Status`, `getTotalDays`) | Nandini *[confirm]* |
| Play Again button and message timing | Shea / JC *[confirm]* |
| Playtest and tune | Everyone |
| GUI smoke test | JC *[confirm]* |
| README reflection, attribution, GenAI note | JC *[confirm]* |

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

We gave Claude our Scope and Specification (class design, method signatures, validation table and test plan) and the Deliverable 3 requirements, and asked it to implement the model, a thin Swing view and controller, JUnit tests, a console driver and the README. We kept the spec's class design and method signatures as written. Claude found that the spec said both "every 5 seconds" and "8 seconds" for arrivals, and followed the constants table. It also removed the unused `secondsWaiting` field, and we kept that change. It ran the 38 tests (all pass), the driver, and launched the GUI for a few seconds without errors. We later asked Claude to add the step of making the drink before completing it, and to keep the "Changes from Spec / Scope to Prototype" table up to date. It added `Menu`, the cup logic and tests, raised the minimum arrival interval from 3 s to 5 s, and its new tests caught a null-handling bug (`Menu.isIngredient(null)` threw a NullPointerException instead of the intended IllegalArgumentException), which it fixed. Then, at our request, it added a 3-second lockout for wrong orders, random gaps between arrivals, and a pause with a "you completed day X" popup between days. It checked the popup with a throwaway script that ticked the game through a day and clicked OK. It then made the line clear at the end of each day, and made each customer left in line at closing cost a life (plus no arrivals in the last 3 seconds of a day). 79 tests pass. For Prototype 2 we then asked it to make the game harder toward the last day (gaps of 2 to 4 seconds on day 7), and it also added a clicks-per-second setting to the driver so we could see whether a perfect player can still win. 81 tests pass. [TEAM: add what you personally reviewed or changed, what you asked Claude about the change feature, and anything you rejected.]
