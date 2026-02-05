# UX Specification: Inventory Check

## Relevant Use Cases:

* UC-01

## Views

* All views are primarily used on a mobile phone or handheld device of similar screen size.

### Select Apparatus

* First screen in the flow.
* Shows the name of the firefighter's station in the header
  * The header has a back button that navigates to Main Menu.
* Shows a list of apparatuses to check for that particular station
  * Sorted by last check from oldest to newest (i.e. recently checked apparatuses are at the bottom)
  * Checked apparatuses are clearly marked so (colors, icons, ec.)
  * Incomplete checks are clearly marked so (colors, icons, etc.)
* Clicking an apparatus navigates to Select Compartment.

```
----------------------------------------------
| [BACK] Test Station 01 Inventory Check     | <-- Header with back button
----------------------------------------------
| Select Apparatus                           | 
| ------------------------------------------ |
| | Engine 1  - Last check: Yesterday      | | <-- Scrollable list of apparatuses to check
| ------------------------------------------ |
| | Truck 1  - Last check: Today           | |
| ------------------------------------------ |
| | Rescue 1  - Last check: Today          | |
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
  * Compartments with all items checked are clearly marked so
* Clicking a compartment navigates to Check Compartment.
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
| | SCBA 1                                 | | <-- Scrollable list of items to check
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
| Mark as Missing                            | <-- Dialog Header
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
