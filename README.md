# Sam Ahold Go to ical
![header image](/.github/go_to_ical.png)

Credentials can be set [here](https://github.com/jord1e/sam_ahold_go_to_ical/blob/master/spring-component/src/main/kotlin/nl/jordie24/samics/SamIcsApplication.kt#L47).

Guaranteed to work for regular Albert Heijn employees.  
I no longer work for Albert Heijn myself, therefore I will not be maintaining this for the foreseeable future. **I will however assist with questions regarding the code via GitHub issues**.

This project is configured to work with MongoDB. It uses the Spring Framework and the Kotlin language.

Once you have entered your credentials and started up for the first time, a 'secret' key (query parameters :D) will be printed to the terminal.

After you have the key you can delete the credentials in your code and restart the application.

You can retrieve your `cid` from the MongoDB database, in the database you can also set your synchronization period.

From there on out you can access your calendar from:
```
http(s)://hostname/sam-go/sync.ics?cid=...&key=...
```

Feel free to fork this repository, the only thing I ask for is attribution to the `ahold-auth-component` module. 
