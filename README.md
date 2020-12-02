# Host App HTML files via Liferay's Document Library


Create a new document folder in Liferay called `"test"`. Let's assume the resulting folderId (you can see it inside the url when editing the folder) is `12345`. In the following we also assume the folder contains `index.html` and `main.js` files;

After deploying this module, go to your instance settings, and find the HosterConfiguration. Add `12345` as an value.


After saving, go check out http://localhost:8080/o/dlhost/test/index.html . You will see your HTML code, congrats!


This does not support subfolders at the moment and is also not very fast while delivering files. It will also rely on the file entries' `VIEW` permission to deal with permission checks, so make sure they are set right. 