# Link

- [URL](https://www.hackerrank.com/challenges/mini-max-sum/problem?isFullScreen=true&h_r=next-challenge&h_v=zen)

# Problem

![](/.uploads/2021-08-02-21-37-28.png)
![](/.uploads/2021-08-02-21-37-50.png)

# Approach

문제 해석

- 주어진 5개 정수에서 4개 합계의 최소값과 최대값 구하여 출력하기

로직

- 주어진 array 에서 최대값, 최소값 찾기
- 전체 합계에서 최대값을 빼서 최소합계를 구하고
- 전체 합계에서 최소값을 빼서 최대합계를 구한다

``` txt
min, max 선언
sum 선언

array 요소에 대해 반복문
    조건문으로 min, max 찾기

return sum-max, sum-min
```

# Code

``` go
func minMaxSum(arr []int32) {
    min, max := arr[0], arr[0]
    sum := int(0)

    for _, v := range arr {
        if v < min {
            min = v
        }
        if v > max {
            max = v
        }

        sum += int(v)
    }

    fmt.Printf("%d %d", sum - int(max), sum - int(min))
}
```

# Answer

(동일)

# Discussion

- 일부 테스트 케이스만 failed 라면 자료형을 의심해보자. int32 가 파라미터인데, constraints 가 1 ≤ arr[i] ≤ 10^9 이므로 주어진 arr 의 각 요소들이 int32 으로 표현 가능할 지라도 일부 연산에 의해 합산된 값은 int32 가 표현할 수 있는 정수를 넘을 수 있다. 그러므로 합산 값인 sum 변수는 int 형으로 바꿔서 연산해야 한다.
- 아래 코드처럼 sum 을 구하는 반복문과 min,max 를 구하는 반복문이 각각 수행될 수 있으나 더 효율적으로 하나로 합칠 수도 있다.

As-Is

``` go
func miniMaxSum(arr []int32) {
    max, min := arr[0], arr[0]
    sum := int64(0)
    
    for _, v := range arr {
        sum += int64(v)
    }
    
    for _, v := range arr {
        if v < min {
            min = v
        }
        if v > max {
            max = v
        }
    }
    
    fmt.Printf("%d %d", sum-int64(max), sum-int64(min))

}
```

To-Be

``` go
func miniMaxSum(arr []int32) {
    min, max := arr[0], arr[0]
    sum := int(0)

    for _, v := range arr {
        if v < min {
            min = v
        }
        if v > max {
            max = v
        }

        sum += int(v)
    }

    fmt.Printf("%d %d", sum - int(max), sum - int(min))
}
```

- array 에서 각 요소들의 합을 구하는 코드

``` go
var sum int
for _, v := arr {
    sum += v
}
```