# Link

- [URL](https://www.hackerrank.com/challenges/birthday-cake-candles/problem?isFullScreen=true&h_r=next-challenge&h_v=zen&h_r=next-challenge&h_v=zen)

# Problem

![](/.uploads/2021-08-02-22-14-51.png)

# Approach

문제 해석

- 주어진 양초 array 에서 가장 높은 수의 개수 구하기

로직

``` txt
반복문으로 최대값 구하기
    구하면서 개수 카운팅하기
```

# Code

``` go
func birthdayCakeCandles(candles []int32) int32 {
    max := candles[0]
    maxCnt := 0
    
    for _, v := range candles {
        if v > max {
            max = v
            maxCnt = 1
        } else if v == max {
            maxCnt++  
        }
    }
    
    return int32(maxCnt)
}
```

# Answer

(동일)

# Discussion

- arr 중에 min 혹은 max 를 구할 때는, 초기 min, max 를 선언할 때 arr[0] 으로 하자.

```go
// e.g.
maxLen := candles[0]
```