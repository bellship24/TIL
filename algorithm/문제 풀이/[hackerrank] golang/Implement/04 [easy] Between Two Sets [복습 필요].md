**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [Code](#code)
- [Answer](#answer)
- [Discussion](#discussion)

---

# Link

- [URL](https://www.hackerrank.com/challenges/between-two-sets/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-03-09-57-20.png)
![](/.uploads/2021-08-03-09-57-30.png)

# Approach

문제 해석

- 주어진 a, b array 에 대해 주어진 2 가지 조건을 만족하는 숫자의 개수를 출력

로직

``` txt
- 첫 번째 array 들의 최소 공배수를 구함
- 두 번째 array 들의 최대 공약수를 구함
- 최소 공배수의 배수 ( 최소 공배수 * 1, 최소 공배수 * 2...)를 해서 최대 공약수를 나누었을 때 나머지가 0인 인수들을 체크
```

예제

``` txt
a = 3, b = 9
GCD(3, 9)
    GCD(9, 3%9=3)
        GCD(3, 3%3=0)  
            = 3
LCM(3, 9)
    3 * 9 / GCD(3, 9)=3
        = 27/3 = 9

a = 10, b = 12
GCD(10, 12)
    GCD(12, 10%12=10)
        GCD(10, 12&10=2)
            GCD(2, 10%2=0)
                = 2

```

# Code

``` go
func GCD(a, b int32) int32 {
    if b == 0 {
        return a
    }
    return GCD(b, a%b)
}

func LCM(a, b int32) int32 {
    return a * b / GCD(a, b)
}

func myGcd(ar []int32) int32 {
    rst := ar[0]
    
    for _, v := range ar[1:] {
        rst = GCD(rst, v)
    }
    return rst
}

func myLcm(ar []int32) int32 {
    rst := ar[0]
    for i := range ar[1:] {
        rst = LCM(rst, ar[i])
    }
    return rst
}

func getTotalX(a []int32, b []int32) int32 {
    var cnt, i int32
    f := myLcm(a)
    l := myGcd(b)
    
    for i = f; i <= l; i += f {
        if l%i==0 {
            cnt++
        }
    }
    return cnt
}
```

# Answer

참고

``` go
func GCD(x, y int32) int32 {
 for y != 0 {
  x, y = y, x%y
 }
 return x
}
func GCD(a []int32) int32{
    result:=a[0]
    
    for i:=1; i<len(a);i++ {
        result = GCD(result,a[i])
    } 
    return result
    }
func LCM(a, b int32) int32 {
      return a * b / GCD(a, b)
}

func lcm(a []int32) int32 {
    result := a[0]
     for i := 0; i < len(a); i++ {
              result = LCM(result, a[i])
      }

      return result
}

/*
 * Complete the getTotalX function below.
 */
func getTotalX(a []int32, b []int32) int32 {
    /*
     * Write your code here.
     */
     var count,i int32
     f := lcm(a)
     l := gcd(b)

     for i=f ; i<=l ; i+=f {
         if l%i==0 {
             count++
         }
     }
   return count
    
}
```

# Discussion

- 문제 해석이 잘 되지 않았다. 예제를 보며 a 배열의 최대공약수와 b 배열의 최소공배수를 구하라는 문제라는 것을 파악했다.
- 배열의 최대공약수/최소공배수를 구할 때는 먼저 임의의 a, b 두 값에 대한 최대공약수/최소공배수를 구하는 함수를 만든다. 전자는 재귀함수로 만들고 후자는 전자함수를 활용한다. 그 다음, 배열에 대해 최대공약수/최소공배수를 구하는 함수를 만들자. 둘 다 반복문을 활용해 배열의 각 요소마다 비교하여 rst 변수에 값을 업데이트하여 리턴하자.
- 최소공배수의 배수가 최대공약수를 나눴을 때 0이 되는 수를 찾는 것도 반복문을 통해 cnt++ 을 해준다.
- GCD = Greatest Common Divisor, 최대공약수
- LCM = Least Common Multiple, 최소공배수