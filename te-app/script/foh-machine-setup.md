# FoH Machine Setup

## Overview

Steps needed to set up libraries / shell utilities on FoH machines (mainly for convenience).

This list is incomplete, but will begin with documenting net-new stuff that we're adding.


## Git

### Git Config

```shell
git config --global push.autoSetupRemote true
```

in `.gitconfig` (machine name, and useful git visualization alias `glog`):

```shell
[user]
	name = TE lighting-1 local
	email = tech@bigbigbig.org
[alias]
	glog = log --graph --decorate --all --pretty=oneline --abbrev-commit
```

### Git completion

```sh
brew install zsh-completions
```


## zshrc

```shell
export PATH=$HOME/bin:/opt/homebrew/bin:/usr/local/bin:$PATH
alias cdt="cd ~/src/code/LXStudio-TE"
alias gco="git checkout"
alias ga="git add"
alias gc="git commit"
alias gl="git log"
alias gs="git status"
alias gb="git branch"
alias gg="git glog"
```