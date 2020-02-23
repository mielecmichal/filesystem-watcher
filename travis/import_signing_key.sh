#!/bin/bash

if [ "${TRAVIS_OS_NAME}" != "osx" ]; then
openssl aes-256-cbc -K $encrypted_92bb71d8d05e_key -iv $encrypted_92bb71d8d05e_iv -in travis/signing_key.gpg.enc -out signing_key.gpg -d
gpg2 --batch --import signing_key.gpg
fi
