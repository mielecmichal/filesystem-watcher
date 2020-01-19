#Contribution Guidelines

TODO

##Cheat sheet

###Creating Travis CI secrets  

```bash
gpg --export-secret-key KEY_NAME > signing_key.gpg

travis login
travis encrypt GPG_PASSPHRASE="foobar" --add
travis encrypt OSSRH_USERNAME="foobar" --add
travis encrypt OSSRH_PASSWORD="foobar" --add
travis encrypt-file signing_key.gpg --add

rm signing_key.gpg
```


