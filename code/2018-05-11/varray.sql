-- Copyright 2018 John Hurst
-- John Hurst (john.b.hurst@gmail.com)
-- 2018-05-10

CREATE TYPE varray_number AS VARRAY(10) OF NUMBER
/

CREATE TABLE t (
  id NUMBER,
  nums varray_number
);

INSERT INTO t VALUES (1, varray_number(120, 230, 340));
INSERT INTO t VALUES (2, varray_number(450, 560, 670));

COMMIT WORK;

SELECT * FROM t;

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

