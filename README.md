This is an incomplete implementation of the WhereIsMyCheese challenge that may be used as a starting point.

The project is meant to be opened with Android Studio. A MapBox token will be required to use the mapping code as is. You can sign up for a free MapBox key at https://www.mapbox.com/signup/ and generate a token at https://www.mapbox.com/account/access-tokens/.

This project has all the necessary assets and styles required to complete the challenge. There is no need to spend too much time on styling.

What you need to do:

1) Edit the `CheesyService` service to get location updates.
2) Bind the `MainActivity` with the `CheesyService`.
3) Have the `CheesyService` run as a foreground service (visible in the notifications).
4) Keep a list of `CheesyTreasures` stored in a variable on the `CheesyService`. They are created in the `MainActivity.createCheesyNote` function. 
5) Fire an event when the device GPS is within 50 meters of a `CheesyNote` which displays the note, and removes the note.

Feel free to change the structure and naming of the code. 
