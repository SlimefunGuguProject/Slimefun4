# Slimefun Legacy Item Doctor — GitHub Drop-In

This archive is arranged from the repository root. Extract it into the root of your
`Slimefun-Legacy` GitHub checkout and allow the `src/` folders to merge.

## Included files

- `src/main/java/io/github/thebusybiscuit/slimefun4/core/commands/subcommands/DoctorCommand.java`
- `src/main/java/io/github/thebusybiscuit/slimefun4/core/services/stability/ItemDoctorService.java`
- `src/main/java/io/github/thebusybiscuit/slimefun4/core/services/stability/ItemDoctorText.java`
- `src/main/java/io/github/thebusybiscuit/slimefun4/core/services/stability/ItemPresentationDoctor.java`
- `src/test/java/io/github/thebusybiscuit/slimefun4/core/services/stability/TestItemDoctorText.java`

## Important status

These are the five loose source files produced during the previous Item Doctor work.
They are organized correctly for GitHub, but they are **not a complete build-ready
stability release by themselves**.

The current files still reference integration pieces that are not included here,
including `ItemDoctorReport`, service initialization/getters, command registration,
permission/config entries, and supporting API changes such as limited-use and spawner
presentation refresh methods.

Do not deploy a JAR built from only this archive to the production server. This package
is intended to preserve and organize the existing work so the remaining integration can
be completed in the main repository.

## Extraction

From the repository root:

```bash
unzip Slimefun-Legacy-Doctor-GitHub-Drop-In.zip
```

Then review with:

```bash
git status --short
git diff --stat
```
