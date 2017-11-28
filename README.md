# Maws

Tooling around managing access to AWS accounts.  Especially wrt federated accounts.

# Quick Start

- Goto the Client Installation section to get up and running.

## Overview

* This is all early days - here be dragons *

To manage your AWS estate it is best practice to have a top-level or root level AWS account which pretty much does nothing except be the end-point for consolidated billing, be the place you buy reserved instances, and manage users and groups via IAM.

You then create sub-accounts in which your infrastructure resides organized how you see fit.  Typically per-largish-project and again split based on whether it's prod or non-prod.

Having users in the root account allows single-sign-on across all the sub-accounts if federated roles are correctly setup.

```
Dancingfrog Root AWS account
|
\-> kermit project prod
|
\-> witan non-prod
|
\-> piggy prod
|
\-> piggy non-prod
|
.
.
```

There are two parts to this code base.

A set of functions that setup and manage users, groups and roles.  At present these are called from the REPL.  They work across multiple accounts and will create suitable federated roles for your users to use.  See `dancingfrog/etc` in the `examples/` dir for how a configuration may look.

The second part is a client for users to either generate a set of temporary credentials they can export to use on the command line or open a console window for the accounts they wish to use.

## Client Installation

The easiest way to install the client is to grab the binary from here:

- https://github.com/MastodonC/maws/releases/download/v1.0/maws

Alternately checkout the repo and run `lein bin` will install a full executable to `~/bin/maws`.

Next you will need to setup `~/.aws/etc/client.edn`

```
{:user "kermit@muppets.com" ;; your root AWS account user name
 :trusted-profile "theatre" ;; The profile name in ~/.aws/config for the AWS top-level root account
 :trusted-account-id "165234343443
 :trusted-role-readonly "TheatreReadOnly"
 :trusted-role-admin "TheatreAdmin"
 :account-ids {:theatre "13468443463"
               :fraggle-prod "82374874734"
               :fraggle-nonprod "234324565"
               :sandpit "234524534"}}
```

Ideally the list of accounts will be somewhere you can easily pull down the information.  For Mastodonc people that's here:-

https://github.com/MastodonC/maws-etc/blob/master/client.edn

Alter for your own user.

## Client Usage

To see options and actions:

    maws -h

The current CLI is somewhat tied to how you set up your roles and groups.

### Opening the AWS web Console

This will generate a 12hr session on the Web console to the `fraggle-prod` account

    maws console -a fraggle-prod


No password is needed as it uses your AWS keys to generate a login URL.  You can display that URL with

    maws console -a fraggle-prod -d

The URL displayed is usable for 15mins.  And it can be shared with anyone to grant them access.  Also handy if you use browser profiles etc to have multiple consoles open at once.

The federation is typically setup so that full Administrative access requires MFA. Use the following to open up the Console.

    maws console-admin -a fraggle-prod -m 345784

### Exporting Environment Vars

A lot of tooling uses ENV vars to configure the connection to AWS.  Federated credentials work to this also.  These credentials are limited by Amazon to maximum duration of 1hour.

You can generate read only credentials with

    maws env -a fraggle-prod

The output will look something like this

    export AWS_ACCESS_KEY_ID=ASIAJ6YF3B...;
    export AWS_SECRET_ACCESS_KEY=jJMO7zaQVDwT4Y...;
    export AWS_SESSION_TOKEN=FQoDYXdz9X5xgU=...;
    export AWS_SECURITY_TOKEN=FQoDYXdzEHgaDH8...;

Cut'n'paste to your prompt or wrap in an eval to make immediately active.

    eval `maws env -a fraggle-prod`

To make credentials that have full admin access use

    maws env-admin -a fraggle-prod -m 345782

### Aliasing

To make things a bit nicer you can run

    maws aliases

Which will give

    alias console-theatre='maws console -a theatre';
    alias admin-console-theatre='maws console-admin -a theatre -m ';
    alias env-theatre='maws env -a theatre';
    alias admin-env-theatre='maws env-admin -a theatre -m ';
    alias console-fraggle-prod='maws console -a fraggle-prod';
    alias ....

Again you can wrap the output in an eval loop and use tab completion on `console-<TAB>` or `console-admin-<TAB>` to open up a browser.  Similar for env exporting `env-<TAB>` and `env-admin<TAB>`.

# Adding a new Account for Federation

Modify the `maws-etc` repo like this commit to add the appropriate data.

https://github.com/MastodonC/maws-etc/commit/a6d11d10fdfbf51d325702382845393069ccea44

Then in a `maws` REPL in the `maws.iam` namespace run

```
(create-account-roles (config) (first {:momondo-kafka [{:name "MastodoncReadOnly"
                                                                  :trusted-account :mastodonc
                                                                  :managed-policy-names ["ReadonlyAccess"]
                                                                  :assume-role-policy-template "assume-role-policy"}
                                                                 {:name "MastodoncAdmin"
                                                                  :trusted-account :mastodonc
                                                                  :managed-policy-names ["AdministratorAccess"]
                                                                  :assume-role-policy-template "assume-role-policy-mfa"}]}))
```

Make sure to communicate out the `client.edn` account addition e.g

```
{:user "matt.ford@mastodonc.com" ;; your Mastodonc AWS account name
 :trusted-profile "mastodonc" ;; The profile name in ~/.aws/config for the Mastodonc AWS account
 :trusted-account-id "165664414043"
 :trusted-role-readonly "MastodoncReadOnly"
 :trusted-role-admin "MastodoncAdmin"
 :account-ids {:mastodonc "165664414043"
               :witan-prod "720433613167"
               :est-nonprod "546282451595"
               :mc-ops-sandpit "201352650455"
               :NEWACCOUNT "095236449097"}}
```

# Adding a new User

Modify the `maws-etc` repo to add the user to groups appropriately.  See this commit for an example:

https://github.com/MastodonC/maws-etc/commit/dea6a89049b5c49f29be5facf61f40d1bf5eab8c?w=1

In a `maws` REPL run

```clojure
(create-user "username" "password")
(create-groups)
```

## Todo

Cough Cough, ahem.

- Code review
- Code clean-up
- Add sub-account creation functionality
- Add CLI interface to AWS account setup
- Split out cli+ to separate library
- Error handling
- Function idempotency (sort of; don't let group creation fail if run twice for example)
- Testing
- Investigate switching out to YAML/JSON (necessary for wider adoption)
- Add a planning phase and apply phase Terraform style.

## License

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
