# Link

- [URL](https://www.hackerrank.com/challenges/diagonal-difference/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-02-11-31-20.png)
![](/.uploads/2021-08-02-11-31-33.png)

# Approach

로직

``` txt
sum 변수 선언

왼쪽 -> 오른쪽 대각선 값들을 sum 에 더하기
오른쪽 -> 왼쪽 대각선 값들을 sum 에 빼기

return |sum|
```

# Code

``` go
var sum int32 = 0
for i := range arr {
    sum += arr[i][i] - arr[i][len(arr) - 1 - i]
}
return int32(math.Abs(float64(sum)))
```

# Answer

(동일)

# Discussion

- array 의 1차원 인덱스에 따라 반복문을 돌리는 법

``` go
for i := range arr {
    // code here
}
```

- array 의 1차원 n 번째 2차원 인덱스에 따라 반복문을 돌리는 법

``` go
for i := range arr[n] {
    // code here
}
```

- int 의 절대값 표현 방법

``` go
package main

import (
    "math"
)

func main() {

var v int32 := -4

fmt.Println(math.Abs(float64(v)))
}
```