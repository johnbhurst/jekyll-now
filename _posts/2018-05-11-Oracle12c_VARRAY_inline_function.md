---
layout: post
title: Oracle Database - inline PL/SQL functions and VARRAYs
description: Using Oracle 12c inline PL/SQL functions to manipulate VARRAYs.
category: oracle
tags: [oracle, 12c, database, sql, pl/sql, varray]
---

# Inline PL/SQL functions in Oracle 12c

Oracle 12c introduced the ability to define a PL/SQL function inline in an SQL query.
The feature uses the `WITH` keyword, similarly to standard SQL's inline table expressions.

Here's the example from [Oracle's documentation](https://docs.oracle.com/database/121/SQLRF/statements_10002.htm#BABJFIDC):

``` sql
WITH
 FUNCTION get_domain(url VARCHAR2) RETURN VARCHAR2 IS
   pos BINARY_INTEGER;
   len BINARY_INTEGER;
 BEGIN
   pos := INSTR(url, 'www.');
   len := INSTR(SUBSTR(url, pos + 4), '.') - 1;
   RETURN SUBSTR(url, pos + 4, len);
 END;
SELECT DISTINCT get_domain(catalog_url)
  FROM product_information;
/
```

# VARRAYs

This feature can be very handy for complex data.

In my work we've used `VARRAY` columns to pack arrays of numeric data into a single column:

``` sql
CREATE TYPE varray_number AS VARRAY(10) OF NUMBER
/
```

We can use this as a regular column type in a table:

``` sql
CREATE TABLE t (
  id NUMBER,
  nums varray_number
);

INSERT INTO t VALUES (1, varray_number(120, 230, 340));
INSERT INTO t VALUES (2, varray_number(450, 560, 670));

COMMIT WORK;

SELECT * FROM t;

        ID NUMS
---------- ----------------------------
         1 VARRAY_NUMBER(120, 230, 340)
         2 VARRAY_NUMBER(450, 560, 670)
```

But this data structure cannot be accessed using SQL - we need to use PL/SQL.
We have written a PL/SQL package for a bunch of standard functions.

# Using an inline function to manipulate VARRAYs in SELECT

But what about ad-hoc queries and data fixes?

Until 12c these would have required writing new (possibly temporary) package functions.
With inline functions it's much better.

For example:

``` sql
WITH FUNCTION scale(v varray_number, factor NUMBER) RETURN varray_number IS
  result varray_number;
BEGIN
  result := varray_number();
  result.EXTEND(v.COUNT);
  FOR i IN 1..v.COUNT LOOP
    result(i) := v(i) * factor;
  END LOOP;
  RETURN result;
END;
SELECT id, scale(nums, 100)
FROM   t;
/

        ID SCALE(NUMS,100)
---------- ----------------------------------
         1 VARRAY_NUMBER(12000, 23000, 34000)
         2 VARRAY_NUMBER(45000, 56000, 67000)
```

# UPDATEs

We can also use this for `UPDATE`s, though the syntax is a little awkward.
I could not get inline functions to work in front of the `UPDATE` keyword itself, only in front of `SELECT`.
So I had to write my `UPDATE` using a `SELECT`.
Also, I learned at [Oracle Base](https://oracle-base.com/articles/12c/with-clause-enhancements-12cr1) that it is necessary to use a `WITH_PLSQL` hint too.

``` sql
UPDATE /*+WITH_PLSQL*/ t
SET    t.nums = (
  WITH FUNCTION scale(v varray_number, factor NUMBER) RETURN varray_number IS
    result varray_number;
  BEGIN
    result := varray_number();
    result.EXTEND(v.COUNT);
    FOR i IN 1..v.COUNT LOOP
      result(i) := v(i) * factor;
    END LOOP;
    RETURN result;
  END;
  SELECT scale(t.nums, 100) FROM dual
);
/

SELECT * FROM t;

        ID NUMS
---------- ----------------------------------
         1 VARRAY_NUMBER(12000, 23000, 34000)
         2 VARRAY_NUMBER(45000, 56000, 67000)
```

# Conclusion

In my opinion, it's best to use this feature judiciously.
Putting common logic into packages avoids duplicating code and allows it to be shared by queries, views and procedures.
Packages should still be the default choice for an applications's PL/SQL code.
The inline function feature is most appropriate for ad-hoc queries and data updates.

As with all features, this one should be assessed on its fit for your particular situation and requirements.

The full code for this post is [here](/code/2018-05-11/varray.sql).
