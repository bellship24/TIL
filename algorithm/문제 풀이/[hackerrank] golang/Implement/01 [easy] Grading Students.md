# Link

- [URL](https://www.hackerrank.com/challenges/grading/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-03-03-04-57.png)
![](/.uploads/2021-08-03-03-05-21.png)

# Approach

문제 해석

- 학생들의 grade 를 round 여부에 맞게 최종 산출하라

로직

``` txt
var rst []int32
for i, v := range beforeGradeArr
    if v % 5 < 3 {
        v += 5
    }
    rst[i] = v
```

반올림 여부 확인

``` txt
71 % 5 = 1 -> 71 (x)
72 % 5 = 2 -> 72 (x)
73 % 5 = 3 -> 75 (o)
74 % 5 = 4 -> 75 (o)
75 % 5 = 0 -> 75 (x)
```

- 5로 나눈 나머지가 3 or 4 일 때, 반올림
- 나머지가 3일 때, +2
- 나머지가 4일 때, +1

# Code

``` go
func gradingStudents(grades []int32) []int32 {
    for i, v := range grades {
        if (v >= 38) && (3 <= v%5) && (v%5 <= 4) {
            grades[i] += 5 - v%5
        }
    }
    return grades
}
```

# Answer

(동일)

# Discussion

- 나머지 구하는 법 : `n%m` : n 을 m 으로 나눈 나머지
- 문제 세밀한 해석

``` txt
- Every student receives a grade in the **inclusive** range from 0 to 100
  → 모든 학생들은 0 부터 100 까지 **포함된** 범위 내에서 등급을 받는다.
- Sam is a professor at the university and likes to **round** each student's grade
  → sam 은 교수이며 각 학생들의 등급을 **반올림** 하는 것을 좋아한다.
- If the difference between the grade and **the next multiple of 5** is **less than 3**, round grade up to the next multiple of 5.
  → 그 등급과 **등급 보다 큰 다음 5 배수** 와의 차이가 3 보다 작다면(**3 미만**), 등급을 다음 5 배수로 반올림한다.
- If the value of grade **is less than 38**, no rounding occurs as the result will still be a failing grade.
  → 등급 값이 **38 미만**이면 결과가 여전히 낙제 등급이므로 반올림이 발생하지 않습니다.
- Each line i of **the n subsequent lines** contains a single integer
  → **n 개의 후속 라인**의 각 라인 i는 단일 정수를 포함합니다.
```