**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [First my code](#first-my-code)
- [Refined my code](#refined-my-code)
- [Other's code](#others-code)
- [Discussion](#discussion)
  - [golang 에서 while 문](#golang-에서-while-문)
  - [x^y 같은 제곱 표현 : math.Pow()](#xy-같은-제곱-표현--mathpow)

---

# Link

- [URL](https://www.hackerrank.com/challenges/sherlock-and-squares/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-05-01-23-41.png)

# Approach

문제 요약

- 주어진 a <= x <= b 범위 내에서 square integer 의 개수를 구해라.

문제 해석

- 1 부터 반복문으로 i 를 돌렸을 때, 제곱한 값이 b 를 넘지 않을 때 까지 돌리며 a <= i^2 <= b 안에 들어가면 iCnt++ 하고 반복문 완료 시에 출력
- 이 문제의 연산 중에 int32 로 해결 가능
- q : test case 개수
- q 번 만큼의 각 라인 : a, b

예제

``` txt
a = 24
b = 49
일 때,
square integer 는 5^2 = 25, 6^2 = 36, 7^2 = 49 로써 총 3개이다.
```

로직

``` txt
- rstCnt 선언
- 1 부터 반복문 i
    - if a < i^2 < b 일 때
        rstCnt++
        - else if i^2 > b 일 때
            return rstCnt
```

로직 적용한 예제

``` txt
- Input
    2
    3 9
    17 24

- 3 9
    1^2 = 1
        1 < 3 이므로 그냥 넘어감

    2^2 = 4
        3 <= 4 <= 9 이므로
            rstCnt++

    3^3 = 9
        3 <= 9 <= 9 이므로
            rstCnt++

    4^4 = 16
        16 > 9 이므로
            return rstCnt

```

# First my code

``` go
func squares(a int32, b int32) int32 {
    var rstCnt int32
    v := 0
    
    for i := 0; i < 1; {
        v++
        if (float64(a) <= math.Pow(float64(v), 2.0)) && (math.Pow(float64(v), 2.0) <= float64(b)) {
            rstCnt++
        } else if (math.Pow(float64(v), 2.0) >= float64(b)) {
            return rstCnt
        }
    }
    return rstCnt
}
```

# Refined my code

# Other's code

``` go
func squares(a int32, b int32) int32 {
        
        return int32(math.Floor(math.Sqrt(float64(b)))-math.Ceil(math.Sqrt(float64(a))))+1
}
```

4 9

2 3

# Discussion

## golang 에서 while 문

```go
package main

import "fmt"

func main() {
		// while 반복문
		n := 0
		for n < 10 {
				fmt.Println(n)
				n++
		}
}
```

## x^y 같은 제곱 표현 : math.Pow()

```go
package main

import (
  "fmt"
  "math"
  // "strings"
)

func main() {
    fmt.Println(math.Pow(2.0, 4.0))
}

/* Output
16
*/
```