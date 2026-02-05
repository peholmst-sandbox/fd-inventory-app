# UX Specification: Inventory Check

## Relevant Use Cases:

* UC-01

## Views

* All views are primarily used on a mobile phone or handheld device of similar screen size.

### Select Apparatus

* First screen in the flow.
* Shows the name of the firefighter's station in the header
  * The header has a back button that navigates to Main Menu.
* If there is an in-progress check for any apparatus, shows a resume banner at the top
  * Shows which apparatus the check is for
  * "Resume" button takes user directly to Select Compartment for that apparatus
  * Visible to all users, as the entire crew can participate in checking an apparatus
* Shows a list of apparatuses to check for that particular station
  * Sorted by last check from oldest to newest (i.e. recently checked apparatuses are at the bottom)
  * Checked apparatuses are clearly marked so (colors, icons, ec.)
  * Incomplete checks are clearly marked so (colors, icons, etc.)
  * If another user is currently checking an apparatus, shows their name(s) below the last check info
* Clicking an apparatus navigates to Select Compartment.

```
----------------------------------------------
| [BACK] Test Station 01 Inventory Check     | <-- Header with back button
----------------------------------------------
| ------------------------------------------ |
| | Resume your check on Engine 1 [RESUME] | | <-- Resume banner (if any apparatus has in-progress check)
| ------------------------------------------ |
| Select Apparatus                           |
| ------------------------------------------ |
| | Engine 1  - Last check: Yesterday      | | <-- Scrollable list of apparatuses to check
| | Being checked by: John Doe             | | <-- Shows who is currently checking (if any)
| ------------------------------------------ |
| | Truck 1  - Last check: Today           | |
| ------------------------------------------ |
| | Rescue 1  - Last check: Today          | |
| | Being checked by: Jane, Bob            | |
| ------------------------------------------ |
|                                            |
----------------------------------------------
```

### Select Compartment

* Shows the name of the apparatus in the header
  * The header has a back button that navigates back to Select Apparatus
* Shows a progress bar indicating how many compartments have been checked
* Shows a list of compartments to check for that particular apparatus
  * Order is predefined and always the same (technicians can configure this)
  * Each item shows how many items have been checked in the compartment during this inventory check
  * If another user is currently checking a compartment, shows their name beneath the item count
  * Compartments with all items checked are clearly marked so
* Clicking a compartment navigates to Check Compartment.
  * If someone else is currently checking that compartment, navigates to Check Compartment (Read-Only Mode) instead
* Clicking the View Summary button in the footer navigates to View Summary

```
----------------------------------------------
| [BACK] Engine 1 Inventory Check            | <-- Header with back button
----------------------------------------------
| Select Compartment                         |
|                                            |
| Checked: 0 of 5 compartments               |
| [========================================] | <-- Progress bar
| ------------------------------------------ |
| | Left Side - Front                      | | <-- Scrollable list of compartments
| | Checked: 0 of 20 items                 | |
| | Checking: John Doe                     | | <-- Shows who is currently checking (if any)
| ------------------------------------------ |
| | Right Side - Front                     | |
| | Checked: 0 of 10 items                 | |
| ------------------------------------------ |
| | Rear Compartment                       | |
| | Checked: 0 of 10 items                 | |
| ------------------------------------------ |
| | Cab Interior                           | |
----------------------------------------------
| [ VIEW SUMMARY ]                           | <-- Footer
----------------------------------------------
```

### Check Compartment

* Shows the name of the apparatus and compartment in the header
  * The header has a back button that navigates back to Select Compartment
* Shows a progress bar indicating how many items have been checked
* Shows a list of items in that particular compartment
  * Order is predefined, but the list is effectively split in two parts: unchecked items are at the top, checked ones at the bottom
  * Each item shows the display name, type name, and serial number.
  * Items marked as crew-owned show a "CREW" badge to distinguish them from department equipment
    * Crew-owned items are verified the same way as department equipment
    * Issues for crew equipment are flagged differently in the system (handled backend, not visible in UX)
  * Each item has a badge that shows the verification status of the item in this inventory check
    * The badge is color coded
  * Each item has three buttons:
    * Present: Marks the item as present
    * Missing: Opens dialog Mark as Missing
    * Damaged: Opens dialog Mark as Damaged
* Whenever an item has been checked, it is automatically moved to the bottom half of the list, and the next unchecked moves up.
  * When going through the list in order, a user should never need to scroll it
* Note: The Scan barcode function is intentionally left out from this first version.

```
----------------------------------------------
| [BACK] Engine 1 - Left Side Front          | <-- Header with back button
----------------------------------------------
| Checked: 0 of 20 items                     |
| [========================================] | <-- Progress bar
| ------------------------------------------ |
| | SCBA 1                          [CREW] | | <-- Scrollable list of items; CREW badge for crew-owned
| | Self-contained breathing apparatus     | |
| | S/N: SCBA-2024-001                     | |
| | NOT CHECKED                            | | <-- Status badge
| | [PRESENT] [MISSING] [DAMAGED]          | | <-- Action buttons
| ------------------------------------------ |
| | SCBA 2                                 | |
| | Self-contained breathing apparatus     | |
| | S/N: SCBA-2024-002                     | |
| | NOT CHECKED                            | |
| | [PRESENT] [MISSING] [DAMAGED]          | |
| ------------------------------------------ |
----------------------------------------------
```

#### Consumable Item Card

Consumable items (items tracked by quantity rather than individual serial numbers) use a different card layout:

* Same header information (display name, type name)
* Shows expected quantity vs actual quantity
* Instead of Present/Missing/Damaged buttons: stepper control with +/- buttons
* Status badge shows:
  * "OK" when actual quantity matches expected quantity
  * "DISCREPANCY" when quantities differ
* If discrepancy exceeds 20%, a notes field appears and is required (per BR-05)

```
----------------------------------------------
| | Batteries (AA)                         | |
| | Consumable                             | |
| | Expected: 24 | Actual: [–] 20 [+]      | | <-- Stepper control for quantity
| | DISCREPANCY                            | | <-- Status badge
| | Notes*                                 | | <-- Required when discrepancy >20%
| | |------------------------------------| | |
| | | Explain the shortage...            | | |
| | |------------------------------------| | |
| ------------------------------------------ |
```

### Check Compartment (Read-Only Mode)

* Shown when entering a compartment that is being checked by another user
* Shows a warning banner at the top identifying who is currently checking
* Shows the same item list as Check Compartment, but items are not actionable (no buttons)
* Item statuses update in real-time as the other user checks items
* Footer contains:
  * Back button: returns to Select Compartment
  * Take Over button: opens Dialog - Confirm Take Over

```
----------------------------------------------
| [BACK] Engine 1 - Left Side Front          | <-- Header with back button
----------------------------------------------
| ------------------------------------------ |
| | ⚠ This compartment is being checked   | | <-- Warning banner
| |   by John Doe                          | |
| ------------------------------------------ |
| Checked: 5 of 20 items                     |
| [========================================] | <-- Progress bar
| ------------------------------------------ |
| | SCBA 1                                 | | <-- Read-only item list (no action buttons)
| | Self-contained breathing apparatus     | |
| | S/N: SCBA-2024-001                     | |
| | NOT CHECKED                            | |
| ------------------------------------------ |
| | SCBA 2                                 | |
| | Self-contained breathing apparatus     | |
| | S/N: SCBA-2024-002                     | |
| | PRESENT                                | | <-- Updated in real-time
| ------------------------------------------ |
----------------------------------------------
| [ BACK ]                    [ TAKE OVER ]  | <-- Footer
----------------------------------------------
```

### Dialog: Mark as Missing

* The dialog contains the details about the missing item
  * Display name, item type, serial number (same as in the item list)
* The user is required to enter details about the missing item
* Clicking Cancel closes the dialog without actions taken
* Clicking Confirm Missing marks the item as missing. The user can then proceed with the inventory check.

```
----------------------------------------------
| Mark as Missing                            | <-- Dialog Header
----------------------------------------------
| ------------------------------------------ |
| | SCBA 1                                 | | <-- Item details
| | Self-contained breathing apparatus     | |
| | S/N: SCBA-2024-001                     | |
| ------------------------------------------ |
| Notes*                                     |
| |----------------------------------------| | 
| | Where was it last seen? Any details    | | <-- Text area with placeholder
| | about the missing item...              | |
| |                                        | | 
| ------------------------------------------ |
| [CANCEL] [CONFIRM MISSING]                 | <-- Dialog footer with buttons
----------------------------------------------
```

### Dialog: Mark as Damaged

* The dialog contains the details about the damaged item
    * Display name, item type, serial number (same as in the item list)
* The user is required to enter details about the damaged item
* Clicking Cancel closes the dialog without actions taken
* Clicking Confirm Damaged marks the item as damaged. The user can then proceed with the inventory check.

```
----------------------------------------------
| Mark as Damaged                            | <-- Dialog Header
----------------------------------------------
| ------------------------------------------ |
| | SCBA 1                                 | | <-- Item details
| | Self-contained breathing apparatus     | |
| | S/N: SCBA-2024-001                     | |
| ------------------------------------------ |
| Notes*                                     |
| |----------------------------------------| |
| | Describe the damage, severity, and if  | | <-- Text area with placeholder
| | the item is still usable...            | |
| |                                        | |
| ------------------------------------------ |
| [CANCEL] [CONFIRM DAMAGED]                 | <-- Dialog footer with buttons
----------------------------------------------
```

### View Summary

* Shows the name of the engine in the header
  * The header has a back button that navigates back to Select Compartment
* The view shows a mini-dashboard with the following stats:
  * Total number of items in the engine
  * Total number of present items
  * Total number of reported issues (damaged or missing)
  * Total number of remaining items in the current check
* Clicking Abandon Check asks for confirmation (dialog), then abandons the check and navigates back to Select Apparatus
* Clicking Complete Check asks for confirmation (dialog), then completes the check and navigates back to Select Apparatus
* The user should never need to scroll anything on this screen.

```
----------------------------------------------
| [BACK] Engine 1 Check Summary              | <-- Header with back button
----------------------------------------------
|                                            |
|  ------------------ || ------------------  |
|          20         ||        20           |
|      Total items    ||      Present        |
|  ------------------ || ------------------  |
|           1         ||         0           |
|        Issue        ||     Remaining       |
|  ------------------ || ------------------  |
|                                            |
----------------------------------------------
| [ ABANDON CHECK ] [ COMPLETE CHECK ]       | <-- Footer
----------------------------------------------
```

### Dialog: Confirm Abandon Check

* Shown when user clicks "Abandon Check" on the View Summary screen
* Shows a warning that progress will be lost
* Clicking Cancel closes the dialog and returns to View Summary
* Clicking Confirm Abandon abandons the check and navigates to Select Apparatus

```
----------------------------------------------
| Abandon Check?                             | <-- Dialog Header
----------------------------------------------
|                                            |
| Are you sure you want to abandon this      |
| inventory check?                           |
|                                            |
| ⚠ All progress will be lost.              |
|                                            |
----------------------------------------------
| [CANCEL]              [CONFIRM ABANDON]    | <-- Dialog footer
----------------------------------------------
```

### Dialog: Confirm Complete Check

* Shown when user clicks "Complete Check" on the View Summary screen
* Shows a summary of the check results
* Clicking Cancel closes the dialog and returns to View Summary
* Clicking Confirm Complete completes the check and navigates to Select Apparatus

```
----------------------------------------------
| Complete Check?                            | <-- Dialog Header
----------------------------------------------
|                                            |
| Summary of your inventory check:           |
|                                            |
|   Items checked:  20                       |
|   Issues found:    1                       |
|                                            |
| Are you sure you want to complete this     |
| check?                                     |
|                                            |
----------------------------------------------
| [CANCEL]             [CONFIRM COMPLETE]    | <-- Dialog footer
----------------------------------------------
```

### Dialog: Confirm Take Over

* Shown when user clicks "Take Over" in Check Compartment (Read-Only Mode)
* Shows who is currently checking the compartment
* Warns that the other user will be notified
* Clicking Cancel closes the dialog and returns to read-only mode
* Clicking Confirm Take Over switches the compartment to edit mode and notifies the other user

```
----------------------------------------------
| Take Over Check?                           | <-- Dialog Header
----------------------------------------------
|                                            |
| John Doe is currently checking this        |
| compartment.                               |
|                                            |
| If you take over, they will be notified    |
| and their access will become read-only.    |
|                                            |
----------------------------------------------
| [CANCEL]            [CONFIRM TAKE OVER]    | <-- Dialog footer
----------------------------------------------
```

## Network Error Handling

* When network connection is lost, a banner/toast appears:
  * "Connection lost. Trying to reconnect..."
  * The banner remains visible while reconnection is attempted
* When connection is restored:
  * Shows confirmation: "Connection restored"
  * Banner/toast auto-dismisses after a few seconds
* If unable to reconnect after multiple attempts:
  * Banner changes to: "Unable to connect. Please check your network connection."
  * Does not auto-dismiss; remains until connection is restored or user navigates away

```
----------------------------------------------
| ⚠ Connection lost. Trying to reconnect... | <-- Network error banner (persistent)
----------------------------------------------

----------------------------------------------
| ✓ Connection restored                      | <-- Success banner (auto-dismisses)
----------------------------------------------

----------------------------------------------
| ✗ Unable to connect. Please check your    | <-- Failure banner (persistent)
|   network connection.                      |
----------------------------------------------
```
