# Squeryl Benchmark
=======================

Simple insert and select benchmark using Squeryl Framework (http://squeryl.org/) and either in-memory or Postgresql database.

Data model:
Author - (1-to-Many) - Book - (Many-to-Many, via AwardPresentation) - Award