# Link

- [URL](https://www.hackerrank.com/challenges/plus-minus/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-02-15-08-01.png)

# Approach

문제 해석

- 주어진 array 의 각 요소들이 갖고 있는 부호에 따른 비율 산출
- 소숫점 6 자리수로 표현할 것

로직

``` txt
plus_cnt := 0
zero_cnt := 0
minu_cnt := 0

반복문 arr
    i 번째 요소의 부호에 따라 조건문으로 cnt 에 +=1

각 cnt 의 소수 표현 출력
```

# Code

``` go
func plusMinus(arr []int32) {
    var posCnt, negCnt, zerCnt float32
    
    for _, v := range arr {
        if v > 0 {
            posCnt++
        } else if v < 0 {
            negCnt++
        } else {
            zerCnt++
        }
    }
    
    fmt.Printf("%.6f\n", posCnt/float32(len(arr)))
    fmt.Printf("%.6f\n", negCnt/float32(len(arr)))
    fmt.Printf("%.6f", zerCnt/float32(len(arr)))
}

```

# Answer

(동일)

# Discussion

- decimal : 소수
- 특정 자리수 까지 소수점 표현 방법

``` go
package main

import (
  "fmt"
)

func main() {
  var a float32 = 2.0/4.0
  fmt.Println(a)
  fmt.Printf("%.6f", a)
}
```

- 같은 자료형의 여러 개 변수를 동시에 선언하는 방법

``` go
var a, b, c int32
```

  - 여기서 int, float 의 초기값은 0 이다.